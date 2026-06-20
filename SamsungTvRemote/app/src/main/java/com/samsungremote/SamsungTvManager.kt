package com.samsungremote

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
import java.io.IOException
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
    private val settings: SettingsDataStore,
    private val logger: AppLogger
) {

    companion object {
        private const val PORT_WS = 8001
        private const val PORT_WSS = 8002
        private const val DEFAULT_REMOTE_NAME = "DroidArchitect"
        private const val REMOTE_NAME_PADDED_LENGTH = 17
        private const val HANDSHAKE_TIMEOUT_MS = 8_000L
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
        logger.i("TVMgr", "Connecting to $ip (mac=$mac, hasToken=${resolvedToken != null})")

        // Try several name formats — different firmware versions expect different encodings
        val nameFormats = listOf(
            remoteName.padEnd(REMOTE_NAME_PADDED_LENGTH, ' '),
            """{"name":"$remoteName"}""",
            remoteName
        )
        val encodedNames = nameFormats.map { URLEncoder.encode(it, "UTF-8") }

        val tokenSuffixes = listOfNotNull(
            "",
            if (resolvedToken != null) "&token=$resolvedToken" else null
        )

        val urlsToTry = encodedNames.flatMap { encoded ->
            tokenSuffixes.flatMap { tok ->
                listOf(
                    "ws://$ip:$PORT_WS/api/v2/channels/samsung.remote.control?name=$encoded$tok",
                    "wss://$ip:$PORT_WSS/api/v2/channels/samsung.remote.control?name=$encoded$tok"
                )
            }
        }

        logger.d("TVMgr", "Will try ${urlsToTry.size} URL combinations")

        val errors = mutableListOf<String>()

        for (url in urlsToTry) {
            handshakeAwaiter = null // discard any previous attempt's awaiter

            val awaiter = CompletableDeferred<Result<Unit>>()
            handshakeAwaiter = awaiter

            try {
                val request = Request.Builder().url(url).build()
                logger.d("TVMgr", "Trying $url")
                val ws = httpClient.newWebSocket(request, createListener(ip, mac))
                wsRef.set(ws)

                withTimeout(HANDSHAKE_TIMEOUT_MS) {
                    val result = awaiter.await()
                    result.getOrThrow()
                }

                logger.i("TVMgr", "Connected via $url")
                settings.saveCredentials(ip, mac, resolvedToken)
                return

            } catch (e: Exception) {
                wsRef.getAndSet(null)?.close(1000, "Fallback")
                handshakeAwaiter = null
                val cause = if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    "Timeout"
                } else {
                    e.localizedMessage ?: e::class.simpleName
                }
                logger.w("TVMgr", "Failed: ${url.take(50)}… → $cause")
                errors.add("${url.take(30)}… : $cause")
            }
        }

        logger.e("TVMgr", "All ${urlsToTry.size} attempts failed")
        val msg = "All connection attempts failed:\n${errors.joinToString("\n")}"
        val ex = IOException(msg)
        _connectionState.value = TvConnectionState.Error(ex)
        throw ex
    }

    // ── Disconnect ────────────────────────────────────────────

    fun disconnect() {
        logger.i("TVMgr", "Disconnect requested")
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
        logger.i("TVMgr", "Shutdown (app exit)")
        disconnect()
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
        logger.d("TVMgr", "sendKey: ${key.code}")
        val sent = ws.send(payload)

        if (!sent) {
            logger.e("TVMgr", "sendKey ${key.code} failed \u2014 connection lost")
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
        logger.d("TVMgr", "sendText: \"${text.take(50)}\"")
        ws.send(payload)
    }

    // ── WebSocket listener ────────────────────────────────────

    private fun createListener(ip: String, mac: String) = object : WebSocketListener() {

        override fun onOpen(ws: WebSocket, response: Response) {
            logger.d("TVMgr", "WS onOpen [$ip]")
        }

        override fun onMessage(ws: WebSocket, text: String) {
            onWsMessage(text, ip, mac)
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            logger.d("TVMgr", "WS onClosing [$ip] code=$code reason=$reason")
            ws.close(code, reason)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            logger.d("TVMgr", "WS onClosed [$ip] code=$code reason=$reason")
            if (!wsRef.compareAndSet(ws, null)) {
                logger.d("TVMgr", "WS onClosed stale callback")
                return
            }
            if (handshakeAwaiter != null) return
            _connectionState.update {
                if (it is TvConnectionState.Connected) {
                    TvConnectionState.Disconnected(reason)
                } else it
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            logger.e("TVMgr", "WS onFailure [$ip]: ${t.localizedMessage ?: t::class.simpleName}")
            if (!wsRef.compareAndSet(ws, null)) {
                logger.d("TVMgr", "WS onFailure stale callback")
                return
            }
            val awaiter = handshakeAwaiter
            if (awaiter != null) {
                awaiter.complete(Result.failure(t))
                handshakeAwaiter = null
            } else {
                _connectionState.value = TvConnectionState.Error(t)
            }
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

                    val effectiveToken = token?.takeUnless { it == "null" || it.isBlank() }
                    logger.i("TVMgr", "WS ms.channel.connect device=$deviceName token=${effectiveToken != null}")

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

                "ms.channel.ready" -> {
                    logger.d("TVMgr", "WS ms.channel.ready [$ip]")
                }

                "ms.remote.control" -> {
                    logger.d("TVMgr", "WS ms.remote.control [$ip]")
                }

                else -> {
                    logger.d("TVMgr", "WS unknown event [$ip]: ${event ?: "null"}")
                }
            }
        } catch (e: Exception) {
            logger.w("TVMgr", "WS message parse error: ${e.localizedMessage}")
        }
    }
}
