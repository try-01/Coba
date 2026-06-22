package com.tvhanan.data.network

import android.util.Base64
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import kotlinx.coroutines.channels.Channel
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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class TvWebSocketClient {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentToken: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _tokenReceived = Channel<String>(Channel.CONFLATED)
    val tokenReceived: Channel<String> = _tokenReceived

    private val appNameBase64: String by lazy {
        Base64.encodeToString("TvHanan".toByteArray(), Base64.NO_WRAP)
    }

    suspend fun connect(ip: String, port: Int = 8001, token: String? = null): Result<WebSocket> {
        return suspendCancellableCoroutine { continuation ->
            currentToken = token
            val url = buildUrl(ip, port)
            val request = Request.Builder()
                .url(url)
                .build()

            _connectionState.value = ConnectionState.CONNECTING

            val ws = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _connectionState.value = ConnectionState.CONNECTED
                    if (continuation.isActive) {
                        continuation.resume(Result.success(webSocket))
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _connectionState.value = ConnectionState.ERROR
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(t))
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
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

            if (event == "ms.channel.ready") {
                val data = json.optJSONObject("data")
                val newToken = data?.optString("token")
                if (!newToken.isNullOrEmpty()) {
                    currentToken = newToken
                    _tokenReceived.trySend(newToken)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED
}
