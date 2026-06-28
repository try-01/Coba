package com.tvhanan.ui.remote

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvWebSocketClient
import com.tvhanan.data.network.WakeOnLanUtil
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.tvhanan.data.network.AppLauncher

class RemoteViewModel(
    private val ipAddress: String,
    private val port: Int = 8002,
    private val macAddress: String? = null,
    private val webSocketClient: TvWebSocketClient = TvWebSocketClient(),
    private val preferences: TvPreferences? = null
) : ViewModel() {
    
    private var tokenObserverJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "TvHanan"
    }

    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isMacAvailable = MutableStateFlow(false)
    val isMacAvailable: StateFlow<Boolean> = _isMacAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            val mac = macAddress ?: preferences?.macAddress?.first()
            _isMacAvailable.value = !mac.isNullOrBlank()
        }
    }
    
    fun connect() {
        Log.d(TAG, "connect() called for $ipAddress")
        _errorMessage.value = null
        viewModelScope.launch {
            // getToken() is now synchronous
            val savedToken = preferences?.getToken()
            Log.d(TAG, "savedToken = ${if (savedToken == null) "null" else "exists"}")

            val result = webSocketClient.connectWithFallback(ipAddress, savedToken)

            if (result.isSuccess) {
                Log.d(TAG, "Connection succeeded")
                if (savedToken == null) {
                    tokenObserverJob?.cancel()
                    tokenObserverJob = launch {
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

    /**
     * Memantau token yang baru tersimpan dari pairing.
     * Dipakai NavGraph untuk sinkronisasi token ke SettingsViewModel.
     */
    suspend fun observeNewToken(onNewToken: suspend (String) -> Unit) {
        webSocketClient.tokenReceived
            .filterNotNull()
            .collect { token ->
                onNewToken(token)
            }
    }

    fun sendKey(key: RemoteKey) {
        if (key == RemoteKey.POWER && connectionState.value != ConnectionState.CONNECTED) {
            wakeOnLan()
        } else {
            webSocketClient.sendKey(key)
        }
    }

    fun wakeOnLan() {
        viewModelScope.launch {
            val mac = macAddress ?: preferences?.macAddress?.first()
            if (!mac.isNullOrBlank()) {
                Log.d(TAG, "Mencoba menyalakan TV via WoL (dengan Retry) ke MAC: $mac")
                
                val success = WakeOnLanUtil.sendWakeOnLanWithRetry(mac)
                
                if (success) {
                    launch {
                        kotlinx.coroutines.delay(4000)
                        
                        repeat(4) { attempt ->
                            if (connectionState.value != ConnectionState.CONNECTED) {
                                Log.d(TAG, "Auto-reconnect setelah WOL, percobaan ke-${attempt + 1}")
                                connect()
                                kotlinx.coroutines.delay(4000)
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "Gagal menjalankan WoL: Alamat MAC tidak ditemukan")
            }
        }
    }

    fun launchApp(appId: String) {
        viewModelScope.launch {
            val success = AppLauncher.launch(ipAddress, appId)
            Log.d(TAG, "launchApp($appId) success=$success")
        }
    }

    fun closeApp(appId: String) {
        viewModelScope.launch {
            val success = AppLauncher.close(ipAddress, appId)
            Log.d(TAG, "closeApp($appId) success=$success")
        }
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        tokenObserverJob?.cancel()
        disconnect()
    }
}
