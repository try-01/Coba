package com.tvhanan.data.network

import android.util.Base64
import android.util.Log
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
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

    private val sslClient by lazy {
        try {
            val sslCtx = SSLContext.getInstance("TLS")
            sslCtx.init(null, trustAllCerts, SecureRandom())
            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(8, TimeUnit.SECONDS)
                .sslSocketFactory(sslCtx.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "SSL client init error: ${e.message}")
            null
        }
    }

    private val plainClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(8, TimeUnit.SECONDS)
            .build()
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

            val client = if (port == 8002) (sslClient ?: plainClient) else plainClient
            val request = Request.Builder()
                .url(buildUrl(ip, port, currentToken))
                .header("Origin", "https://localhost:$port")
                .build()

            try {
                val ws = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        Log.d(TAG, "Connected to $ip:$port")
                        _connectionState.value = ConnectionState.CONNECTED
                        sendKey(RemoteKey.HOME)
                        if (continuation.isActive) {
                            continuation.resume(Result.success(ws))
                        }
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        handleMessage(text)
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        val detail = t.message ?: t.javaClass.simpleName
                        Log.e(TAG, "Failed $ip:$port: $detail")
                        if (response != null) Log.e(TAG, "HTTP ${response.code}")
                        _connectionState.value = ConnectionState.ERROR
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(t))
                        }
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "Closed: $code $reason")
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                })

                webSocket = ws

                continuation.invokeOnCancellation {
                    ws.close(1001, "Cancelled")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            } catch (e: Exception) {
                Log.e(TAG, "WS error: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    suspend fun connectWithFallback(ip: String, token: String? = null): Result<WebSocket> {
        Log.d(TAG, "=== Connecting $ip ===")

        for (port in listOf(8002, 8001)) {
            Log.d(TAG, "Trying $ip:$port...")
            val result = connect(ip, port, token)
            if (result.isSuccess) {
                Log.d(TAG, "Success on $ip:$port!")
                return result
            }
            Log.e(TAG, "Failed $ip:$port: ${result.exceptionOrNull()?.message}")
        }
        return Result.failure(Exception("TV tidak merespon"))
    }

    fun sendKey(key: RemoteKey): Boolean {
        return try {
            val payload = SamsungKeyMapper.createKeyPressPayload(key)
            webSocket?.send(payload) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
            false
        }
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
            Log.d(TAG, "Event: ${json.optString("event")}")
            if (json.optString("event") == "ms.channel.ready") {
                val newToken = json.optJSONObject("data")?.optString("token")
                if (!newToken.isNullOrEmpty()) {
                    currentToken = newToken
                    _tokenReceived.value = newToken
                    Log.d(TAG, "Token: $newToken")
                }
            }
        } catch (_: Exception) {}
    }
}
