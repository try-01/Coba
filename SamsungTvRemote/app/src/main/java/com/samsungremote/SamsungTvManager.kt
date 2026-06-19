package com.samsungremote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Core engine that manages the WebSocket Secure (WSS) connection to a
 * Samsung Tizen TV for remote control.
 *
 * ## Connection flow
 * 1. [connect] opens a WSS to `wss://{ip}:8002/api/v2/channels/…`
 * 2. TV responds with `ms.channel.connect` containing a pairing token
 * 3. On first connection the TV shows a PIN on screen; subsequent
 *    connections pass the saved token for automatic pairing
 * 4. [sendKey] transmits a JSON-RPC-style "Click" command
 *
 * Thread safety: all mutable state is accessed only via the internal
 * coroutine scope (Dispatchers.IO). The WebSocket reference is held
 * in an AtomicReference for safe cross-thread read/write.
 */
class SamsungTvManager(
    private val settings: SettingsDataStore
) {

    companion object {
        private const val WSS_PORT = 8002
        private const val DEFAULT_REMOTE_NAME = "DroidArchitect"
        private const val REMOTE_NAME_PADDED_LENGTH = 17
        private const val HANDSHAKE_TIMEOUT_MS = 10_000L
        private const val PING_INTERVAL_SECONDS = 30L
    }

    // ── JSON ──────────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true }

    // ── OkHttp shared client ──────────────────────────────────

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // long-lived WS — no read deadline
        .build()

    // ── Connection state ─────────────────────────────────────

    private val _connectionState = MutableStateFlow<TvConnectionState>(TvConnectionState.Idle)
    val connectionState: StateFlow<TvConnectionState> = _connectionState.asStateFlow()

    private val wsRef = AtomicReference<WebSocket?>(null)

    // Internal scope tied to the manager's lifetime
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var handshakeAwaiter: CompletableDeferred<Result<Unit>>? = null

    // ── Public API ────────────────────────────────────────────

    /**
     * Opens a WSS connection to [ip]:8002.
     *
     * If [token] is null the method first looks for a previously-saved
     * token in DataStore. On first-ever connect the TV will display a
     * pairing prompt; the emitted token from the handshake is
     * automatically persisted for future connections.
     */
    suspend fun connect(
        ip: String,
        mac: String,
        token: String? = null,
        remoteName: String = DEFAULT_REMOTE_NAME
    ) {
        val resolvedToken = token ?: settings.getSavedToken()
        _connectionState.value = TvConnectionState.Connecting

        val awaiter = CompletableDeferred<Result<Unit>>()
        handshakeAwaiter = awaiter

        try {
            val encodedName = URLEncoder.encode(
                remoteName.padEnd(REMOTE_NAME_PADDED_LENGTH, '\u0000'),
                "UTF-8"
            )

            val url = buildString {
                append("wss://$ip:$WSS_PORT/api/v2/channels/samsung.remote.control?name=$encodedName")
                if (resolvedToken != null) append("&token=$resolvedToken")
            }

            val request = Request.Builder().url(url).build()

            val ws = httpClient.newWebSocket(request, createListener(ip, mac))
            wsRef.set(ws)

            // Block until handshake completes or times out
            withTimeout(HANDSHAKE_TIMEOUT_MS) {
                val result = awaiter.await()
                result.getOrThrow()
            }

            // Persist credentials only on success
            settings.saveCredentials(ip, mac, token)

        } catch (e: CancellationException) {
            disconnect()
            throw e
        } catch (e: Exception) {
            disconnect()
            _connectionState.value = TvConnectionState.Error(e)
            throw e
        }
    }

    /**
     * Gracefully closes the WebSocket and resets state.
     */
    fun disconnect() {
        wsRef.getAndSet(null)?.close(1000, "Client disconnect")
        handshakeAwaiter?.complete(Result.failure(IllegalStateException("Disconnected")))
        handshakeAwaiter = null
        _connectionState.update { current ->
            if (current is TvConnectionState.Connected || current is TvConnectionState.Connecting) {
                TvConnectionState.Disconnected("User requested")
            } else current
        }
    }

    /**
     * Sends a single key-press command over the active WebSocket.
     *
     * @throws IllegalStateException if not connected.
     * @throws java.io.IOException if the WebSocket send buffer is full.
     */
    suspend fun sendKey(key: SamsungRemoteKey) {
        val ws = wsRef.get()
            ?: throw IllegalStateException("WebSocket is null — not connected")

        val msg = buildJsonObject {
            put("method", "ms.remote.control")
            putJsonObject("params") {
                put("Cmd", "Click")
                put("DataOfCmd", key.code)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            }
        }

        val payload = json.encodeToString(JsonObject.serializer(), msg)
        val sent = ws.send(payload)

        if (!sent) {
            // WebSocket output buffer is full or socket is closing
            disconnect()
            throw java.io.IOException("WebSocket send returned false — probable connection loss")
        }
    }

    /**
     * Sends a text string via the KEYBOARD virtual remote.
     * The TV must be in keyboard input mode.
     */
    suspend fun sendText(text: String) {
        val ws = wsRef.get()
            ?: throw IllegalStateException("WebSocket is null — not connected")

        val msg = buildJsonObject {
            put("method", "ms.remote.control")
            putJsonObject("params") {
                put("Cmd", "SendInputString")
                put("DataOfCmd", text)
                put("TypeOfRemote", "SendRemoteKey")
            }
        }

        val payload = json.encodeToString(JsonObject.serializer(), msg)
        ws.send(payload)
    }

    /**
     * Performs a deep-clean shutdown: closes the WebSocket, evicts
     * the connection pool, shuts down the dispatcher, and cancels
     * the internal coroutine scope.
     *
     * Call this from [android.app.Activity.finishAndRemoveTask] for
     * zero battery-drain exit.
     */
    fun shutdown() {
        wsRef.getAndSet(null)?.close(1000, "App shutdown")
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        scope.cancel()
        _connectionState.value = TvConnectionState.Disconnected("Shutdown")
    }

    // ── WebSocket listener ────────────────────────────────────

    private fun createListener(ip: String, mac: String) = object : WebSocketListener() {

        override fun onOpen(ws: WebSocket, response: Response) {
            // Connection upgraded — waiting for ms.channel.connect
        }

        override fun onMessage(ws: WebSocket, text: String) {
            onWsMessage(text, ip, mac)
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            ws.close(code, reason)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            wsRef.compareAndSet(ws, null)
            _connectionState.update {
                if (it is TvConnectionState.Connected || it is TvConnectionState.Connecting) {
                    TvConnectionState.Disconnected(reason)
                } else it
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            wsRef.compareAndSet(ws, null)
            val errorState = TvConnectionState.Error(t)
            _connectionState.value = errorState
            handshakeAwaiter?.complete(Result.failure(t))
            handshakeAwaiter = null
        }
    }

    private fun onWsMessage(text: String, ip: String, mac: String) {
        try {
            val root = json.parseToJsonElement(text).jsonObject
            val event = root["event"]?.jsonPrimitive?.content

            when (event) {
                "ms.channel.connect" -> {
                    val data = root["data"]?.jsonObject
                    val token = data?.get("token")?.jsonPrimitive?.content
                    val deviceName = data?.get("clients")?.jsonArray
                        ?.firstOrNull()
                        ?.jsonObject
                        ?.get("deviceName")?.jsonPrimitive?.content

                    // Persist the token for future auto-connect
                    if (token != null) {
                        scope.launch {
                            settings.saveCredentials(ip, mac, token)
                        }
                    }

                    _connectionState.value = TvConnectionState.Connected(
                        ip = ip,
                        mac = mac,
                        deviceName = deviceName ?: "Samsung TV"
                    )
                    handshakeAwaiter?.complete(Result.success(Unit))
                    handshakeAwaiter = null
                }

                "ms.channel.ready" -> {
                    // TV signals the channel is fully initialised — connection is live
                }

                "ms.remote.control" -> {
                    // Echo confirmation of a sent key — no action needed
                }

                else -> {
                    // Unknown event — ignore silently (firmware-specific pings etc.)
                }
            }
        } catch (_: Exception) {
            // Malformed JSON from TV — ignore
        }
    }
}
