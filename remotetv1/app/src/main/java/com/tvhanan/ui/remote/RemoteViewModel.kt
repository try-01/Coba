package com.tvhanan.ui.remote

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvWebSocketClient
import com.tvhanan.data.network.WakeOnLanUtil
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import dagger.Assisted
import dagger.hilt.AssistedInject
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

    // Catatan: showNumpad/toggleNumpad dipertahankan untuk kompatibilitas,
    // tapi RemoteScreen versi mesh/glass saat ini menampilkan numpad secara
    // selalu-terlihat (tidak collapse), sehingga state ini tidak lagi
    // dipakai oleh UI. Aman dibiarkan kalau nanti ingin kembali ke pola
    // collapse, atau bisa dihapus jika dipastikan tidak ada pemanggil lain.
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

    /**
     * Memantau token yang baru tersimpan (baik dari pairing baru maupun
     * yang sudah ada sebelumnya), memanggil [onNewToken] tiap kali nilainya
     * berubah dan tidak null. Dipakai NavGraph untuk sinkronisasi token ke
     * SettingsViewModel tanpa duplikasi logic penyimpanan token.
     */
    suspend fun observeNewToken(onNewToken: suspend (String) -> Unit) {
        _lastSavedToken.filterNotNull().collect { token ->
            onNewToken(token)
        }
    }

    /**
     * Mengirim key ke TV. Catatan: haptic feedback TIDAK dipicu di sini —
     * itu ditangani oleh komponen UI (HapticGlassButton) saat tombol
     * mulai ditekan (onPress), bukan di layer ViewModel. Ini menghindari
     * getar terpicu dua kali (sekali dari UI saat press, sekali lagi
     * dari sini) dan menjaga ViewModel tidak punya concern soal detail
     * presentasi seperti vibration.
     */
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