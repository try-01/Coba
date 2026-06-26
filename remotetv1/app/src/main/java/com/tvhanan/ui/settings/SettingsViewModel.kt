package com.tvhanan.ui.remote

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvWebSocketClient
import com.tvhanan.data.network.WakeOnLanUtil
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import dagger.assisted.Assisted       // DIPERBAIKI: Import yang benar
import dagger.assisted.AssistedInject // DIPERBAIKI: Import yang benar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoteViewModel @AssistedInject constructor(
    @Assisted private val ipAddress: String,
    @Assisted private val port: Int = 8001,
    private val webSocketClient: TvWebSocketClient,
    private val preferences: TvPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "TvHanan"
    }

    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    private val _showNumpad = MutableStateFlow(false)
    val showNumpad: StateFlow<Boolean> = _showNumpad.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun toggleNumpad() {
        _showNumpad.value = !_showNumpad.value
    }

    private val _lastSavedToken = MutableStateFlow<String?>(null)

    fun connect() {
        Log.d(TAG, "connect() called for $ipAddress")
        viewModelScope.launch {
            val savedToken = preferences.getToken()
            Log.d(TAG, "savedToken = ${if (savedToken == null) "null" else "exists"}")

            val result = webSocketClient.connectWithFallback(ipAddress, savedToken)

            if (result.isSuccess) {
                Log.d(TAG, "Connection succeeded")
                if (savedToken == null) {
                    launch {
                        val newToken = webSocketClient.tokenReceived
                            .filterNotNull()
                            .first()
                        preferences.saveToken(newToken)
                        _lastSavedToken.value = newToken
                        Log.d(TAG, "First token saved: $newToken")
                    }
                } else {
                    _lastSavedToken.value = savedToken
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal terhubung ke TV"
                Log.e(TAG, "Connection failed: $errorMsg")
                _errorMessage.value = errorMsg
            }
        }
    }

    suspend fun observeNewToken(onNewToken: suspend (String) -> Unit) {
        _lastSavedToken.filterNotNull().collect { token ->
            onNewToken(token)
        }
    }

    fun sendKey(key: RemoteKey) {
        webSocketClient.sendKey(key)
    }

    fun launchApp(appId: String) {
        webSocketClient.launchApp(appId)
    }

    fun wakeOnLan() {
        val mac = preferences.macAddress.firstOrNull()
        if (mac != null) {
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