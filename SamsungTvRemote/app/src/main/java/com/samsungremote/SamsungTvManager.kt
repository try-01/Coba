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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SamsungTvManager(
    private val settings: SettingsDataStore
) {

    companion object {
        private const val WSS_PORT = 8002
        private const val DEFAULT_REMOTE_NAME = "DroidArchitect"
        private const val REMOTE_NAME_PADDED_LENGTH = 17
        private const val HANDSHAKE_TIMEOUT_MS = 15_000L
        private const val PING_INTERVAL_SECONDS = 30L
    }

    private val json = Json { ignoreUnknownKeys = true }

    // Samsung TVs use self-signed WSS certificates — we must trust them all.
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val trustAllSslSocketFactory: SSLSocketFactory by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }.socketFactory
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .sslSocketFactory(trustAllSslSocketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private val _connectionState = MutableStateFlow<TvConnectionState>(TvConnectionState.Idle)
    val connectionState: StateFlow<TvConnectionState> = _connectionState.asStateFlow()

    private val wsRef = AtomicReference<WebSocket?>(null)

    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var handshakeAwaiter: CompletableDeferred<Result<Unit>>? = null

    // ── Connect ───────────────────────────────────────────────

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

            withTimeout(HANDSHAKE_TIMEOUT_MS) {
                val result = awaiter.await()
                result.getOrThrow()
            }

            settings.saveCredentials(ip, mac, resolvedToken)

        } catch (e: CancellationException) {
            disconnect()
            throw e
        } catch (e: Exception) {
            disconnect()
            _connectionState.value = TvConnectionState.Error(e)
            throw e
        }
    }

    // ── Disconnect ────────────────────────────────────────────

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

    fun shutdown() {
        wsRef.getAndSet(null)?.close(1000, "App shutdown")
        handshakeAwaiter?.complete(Result.failure(IllegalStateException("Shutdown")))
        handshakeAwaiter = null
        scope.cancel()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        _connectionState.value = TvConnectionState.Disconnected("Shutdown")
    }

    // ── Send commands ─────────────────────────────────────────

    suspend fun sendKey(key: SamsungRemoteKey) {
        val ws = wsRef.get()
            ?: throw IllegalStateException("WebSocket is null \u2014 not connected")

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
            disconnect()
            throw java.io.IOException("WebSocket send returned false \u2014 probable connection loss")
        }
    }

    suspend fun sendText(text: String) {
        val ws = wsRef.get()
            ?: throw IllegalStateException("WebSocket is null \u2014 not connected")

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

    // ── WebSocket listener ────────────────────────────────────

    private fun createListener(ip: String, mac: String) = object : WebSocketListener() {

        override fun onOpen(ws: WebSocket, response: Response) { }

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
            _connectionState.value = TvConnectionState.Error(t)
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

                    // If token is "null" as a string, treat it as no-token (first pairing)
                    val effectiveToken = token?.takeUnless { it == "null" || it.isBlank() }

                    if (effectiveToken != null) {
                        scope.launch {
                            settings.saveCredentials(ip, mac, effectiveToken)
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

                "ms.channel.ready" -> { }

                "ms.remote.control" -> { }

                else -> { }
            }
        } catch (_: Exception) { }
    }
}
