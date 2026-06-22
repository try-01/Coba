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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume

class TvWebSocketClient {

    companion object {
        private const val TAG = "TvHanan"
    }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val client by lazy {
        try {
            val sslCtx = SSLContext.getInstance("TLS")
            sslCtx.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .sslSocketFactory(sslCtx.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "SSL init error: ${e.message}", e)
            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    private var webSocket: WebSocket? = null
    private var currentToken: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _tokenReceived = MutableStateFlow<String?>(null)
    val tokenReceived: StateFlow<String?> = _tokenReceived.asStateFlow()

    private val appNameBase64: String by lazy {
        Base64.encodeToString("TvHanan".toByteArray(), Base64.NO_WRAP)
    }

    suspend fun connect(ip: String, port: Int, token: String? = null): Result<WebSocket> {
        return suspendCancellableCoroutine { continuation ->
            currentToken = token
            _connectionState.value = ConnectionState.CONNECTING

            Log.d(TAG, "Connecting to $ip:$port...")

            val request = Request.Builder()
                .url(buildUrl(ip, port, currentToken))
                .header("Origin", "https://localhost:$port")
                .build()

            val ws = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "Connected to $ip:$port")
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
                    Log.e(TAG, "Failed to $ip:$port: ${t.message}")
                    response?.let {
                        Log.e(TAG, "HTTP ${it.code} ${it.message}")
                    }
                    _connectionState.value = ConnectionState.ERROR
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(t))
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "Closed: $code $reason")
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
        Log.d(TAG, "=== Trying to connect $ip ===")
        Log.d(TAG, "Saved token: ${if (token.isNullOrEmpty()) "none" else "exists"}")

        val ports = listOf(8002, 8001)
        for (port in ports) {
            Log.d(TAG, "Trying port $port...")
            val result = connect(ip, port, token)
            if (result.isSuccess) {
                Log.d(TAG, "Connected on port $port!")
                return result
            }
            Log.e(TAG, "Port $port failed, ${result.exceptionOrNull()?.message}")
        }
        return Result.failure(Exception("TV tidak merespon"))
    }

    fun sendKey(key: RemoteKey): Boolean {
        val payload = SamsungKeyMapper.createKeyPressPayload(key)
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
            Log.d(TAG, "Event: $event")

            if (event == "ms.channel.ready") {
                val data = json.optJSONObject("data")
                val newToken = data?.optString("token")
                if (!newToken.isNullOrEmpty()) {
                    currentToken = newToken
                    _tokenReceived.value = newToken
                    Log.d(TAG, "Token saved: $newToken")
                }
            }
        } catch (_: Exception) {
        }
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED
}
