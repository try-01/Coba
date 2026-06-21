package com.example.samsungremote.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.example.samsungremote.utils.ConnectionState

class SamsungTvManager {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var scope = CoroutineScope(Dispatchers.Main + Job())

    private var tvIp: String? = null
    private var tvPort: Int = 8002
    private var token: String? = null

    fun connect(ip: String, port: Int = 8002) {
        tvIp = ip
        tvPort = port
        _connectionState.value = ConnectionState.Connecting
        createWebSocket()
    }

    fun sendCommand(command: String) {
        webSocket?.send(command)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        client = null
        scope.cancel()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun createWebSocket() {
        client = OkHttpClient()
        val url = "ws://${tvIp}:${tvPort}"
        val request = Request.Builder().url(url).build()
        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnecting
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
            }
        })
    }

    companion object {
        fun generatePin(): String {
            val random = (1000..9999).random()
            return random.toString()
        }
    }
}