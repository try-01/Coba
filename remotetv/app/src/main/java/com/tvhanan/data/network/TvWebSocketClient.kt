package com.tvhanan.data.network

import android.util.Base64
import android.util.Log
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume

class TvWebSocketClient {

    companion object {
        private const val TAG = "TvWebSocket"
    }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val sslContext: SSLContext by lazy {
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, trustAllCerts, SecureRandom())
        ctx
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private var webSocket: WebSocket? = null
    private var currentToken: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _tokenReceived = MutableStateFlow<String?>(null)
    val tokenReceived: StateFlow<String?> = _tokenReceived.asStateFlow()

    private val appNameBase64: String by lazy {
        Base64.encodeToString(
            "TvHanan".toByteArray(),
            Base64.NO_WRAP
        )
    }

    suspend fun testRawConnection(ip: String, port: Int): String? {
        return try {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(ip, port), 5000)

            val writer = OutputStreamWriter(socket.getOutputStream())
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            val httpRequest = buildString {
                append("GET /api/v2/channels/samsung.remote.control?name=$appNameBase64 HTTP/1.1\r\n")
                append("Host: $ip:$port\r\n")
                append("Upgrade: websocket\r\n")
                append("Connection: Upgrade\r\n")
                append("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n")
                append("Sec-WebSocket-Version: 13\r\n")
                append("Origin: https://localhost:8001\r\n")
                append("\r\n")
            }

            writer.write(httpRequest)
            writer.flush()

            val response = reader.readLine()
            socket.close()
            response
        } catch (e: Exception) {
            Log.e(TAG, "Raw test failed for $ip:$port", e)
            null
        }
    }

    suspend fun connect(ip: String, port: Int = 8001, token: String? = null): Result<WebSocket> {
        return suspendCancellableCoroutine { continuation ->
            currentToken = token

            _connectionState.value = ConnectionState.CONNECTING

            val request = Request.Builder()
                .url(buildUrl(ip, port, currentToken))
                .header("Origin", "https://localhost:$port")
                .build()

            val ws = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket opened to $ip:$port")
                    _connectionState.value = ConnectionState.CONNECTED
                    sendKey(RemoteKey.HOME)
                    if (continuation.isActive) {
                        continuation.resume(Result.success(webSocket))
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failed to $ip:$port: ${t.message}", t)
                    response?.let { resp ->
                        Log.e(TAG, "HTTP response: ${resp.code} ${resp.message}")
                        resp.body?.let { body ->
                            try {
                                Log.e(TAG, "Body: ${body.string()}")
                            } catch (_: Exception) {}
                        }
                    }
                    _connectionState.value = ConnectionState.ERROR
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(t))
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $code $reason")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            })

            webSocket = ws

            continuation.invokeOnCancellation {
                ws.close(1001, "Cancelled")
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    suspend fun connectWithFallback(ip: String, token: String? = null): Result<WebSocket> {
        Log.d(TAG, "Testing raw connection to $ip:8001...")
        val rawResponse = testRawConnection(ip, 8001)
        Log.d(TAG, "Raw response: $rawResponse")

        val ports = listOf(8001, 8002)
        for (port in ports) {
            Log.d(TAG, "Trying WebSocket on $ip:$port")
            val result = connect(ip, port, token)
            if (result.isSuccess) return result
            Log.e(TAG, "Failed on $ip:$port, trying next...")
        }
        return Result.failure(Exception("TV tidak merespon di port 8001/8002"))
    }

    fun sendKey(key: RemoteKey): Boolean {
        val payload = SamsungKeyMapper.createKeyPressPayload(key)
        Log.d(TAG, "Sending key: ${key.keyCode}")
        return webSocket?.send(payload) ?: false
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun buildUrl(ip: String, port: Int, token: String? = null): String {
        val scheme = if (port == 8002) "wss" else "ws"
        val base = "$scheme://$ip:$port/api/v2/channels/samsung.remote.control?name=$appNameBase64"
        return if (!token.isNullOrEmpty()) "$base&token=$token" else base
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")
            Log.d(TAG, "Received event: $event")

            if (event == "ms.channel.ready") {
                val data = json.optJSONObject("data")
                val newToken = data?.optString("token")
                if (!newToken.isNullOrEmpty()) {
                    currentToken = newToken
                    _tokenReceived.value = newToken
                    Log.d(TAG, "Token received: $newToken")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Message: $text")
        }
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED
}
