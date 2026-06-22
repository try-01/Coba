package com.tvhanan.ui.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvWebSocketClient
import com.tvhanan.data.network.WakeOnLanUtil
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
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

    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    private val _showNumpad = MutableStateFlow(false)
    val showNumpad: StateFlow<Boolean> = _showNumpad.asStateFlow()

    fun toggleNumpad() {
        _showNumpad.value = !_showNumpad.value
    }

    fun connect() {
        viewModelScope.launch {
            val savedToken = preferences?.getToken()
            val result = webSocketClient.connect(ipAddress, port, savedToken)

            if (result.isSuccess && savedToken == null) {
                launch {
                    val newToken = webSocketClient.tokenReceived
                        .filterNotNull()
                        .first()
                    preferences?.saveToken(newToken)
                }
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
