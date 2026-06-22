package com.tvhanan.ui.remote

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvWebSocketClient
import com.tvhanan.data.network.WakeOnLanUtil
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.util.HapticUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RemoteViewModel(
    private val ipAddress: String,
    private val port: Int = 8001,
    private val macAddress: String? = null,
    private val webSocketClient: TvWebSocketClient = TvWebSocketClient(),
    private val preferences: TvPreferences? = null
) : ViewModel() {

    companion object {
        private const val TAG = "TvHanan"
    }

    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    private val _showNumpad = MutableStateFlow(false)
    val showNumpad: StateFlow<Boolean> = _showNumpad.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _remoteSize = MutableStateFlow("fit")
    val remoteSize: StateFlow<String> = _remoteSize.asStateFlow()

    private val _meshEnabled = MutableStateFlow(true)
    val meshEnabled: StateFlow<Boolean> = _meshEnabled.asStateFlow()

    val tvDevice: TvDevice
        get() = TvDevice(ipAddress = ipAddress, macAddress = macAddress, port = port)

    init {
        viewModelScope.launch {
            preferences?.let {
                _remoteSize.value = it.remoteSize.first()
                _meshEnabled.value = it.meshBackground.first()
            }
        }
    }

    fun toggleNumpad() {
        _showNumpad.value = !_showNumpad.value
    }

    fun connect() {
        Log.d(TAG, "connect() called for $ipAddress")
        _errorMessage.value = null
        viewModelScope.launch {
            val savedToken = preferences?.getToken()

            val result = webSocketClient.connectWithFallback(ipAddress, savedToken)

            if (result.isSuccess) {
                Log.d(TAG, "Connection succeeded")
                if (savedToken == null) {
                    launch {
                        val newToken = webSocketClient.tokenReceived
                            .filterNotNull()
                            .first()
                        preferences?.saveToken(newToken)
                        Log.d(TAG, "First token saved: $newToken")
                    }
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal terhubung ke TV"
                Log.e(TAG, "Connection failed: $errorMsg")
                _errorMessage.value = errorMsg
            }
        }
    }

    fun sendKey(key: RemoteKey) {
        HapticUtil.tick()
        webSocketClient.sendKey(key)
    }

    fun wakeOnLan() {
        macAddress?.let { mac ->
            HapticUtil.tick()
            WakeOnLanUtil.sendWakeOnLan(mac)
        }
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
