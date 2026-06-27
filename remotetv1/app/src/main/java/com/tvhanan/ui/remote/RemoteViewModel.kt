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
    
    private var tokenObserverJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "TvHanan"
    }

    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    // tapi RemoteScreen versi mesh/glass saat ini menampilkan numpad secara
    // selalu-terlihat (tidak collapse), sehingga state ini tidak lagi
    // dipakai oleh UI. Aman dibiarkan kalau nanti ingin kembali ke pola
    // collapse, atau bisa dihapus jika dipastikan tidak ada pemanggil lain.

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastSavedToken = MutableStateFlow<String?>(null)
    
    // State baru untuk mendeteksi apakah TV ini mendukung WOL (memiliki MAC terdaftar)
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
        viewModelScope.launch {
            val savedToken = preferences?.getToken()
            Log.d(TAG, "savedToken = ${if (savedToken == null) "null" else "exists"}")

            val result = webSocketClient.connectWithFallback(ipAddress, savedToken)

            if (result.isSuccess) {
                Log.d(TAG, "Connection succeeded")
                if (savedToken == null) {
    tokenObserverJob?.cancel() // Batalkan job pengamat token lama sebelum meluncurkan yang baru
    tokenObserverJob = launch {
        val newToken = webSocketClient.tokenReceived
            .filterNotNull()
            .first()
        preferences?.saveToken(newToken)
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
        // Jika menekan POWER saat TV mati/terputus, kirim paket Wake-on-LAN untuk menyalakan TV
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
                
                // 1. Kirim Magic Packet berulang lewat utility retry agar TV pasti terbangun
                val success = WakeOnLanUtil.sendWakeOnLanWithRetry(mac)
                
                if (success) {
                    // 2. OPTIMASI PREMIUM: Auto-Reconnect Loop
                    // TV Tizen membutuhkan waktu booting ~5-15 detik sebelum server WebSocket internalnya aktif.
                    // Kita buat aplikasi otomatis mencoba menghubungkan kembali setiap 4 detik sebanyak 4 kali percobaan.
                    launch {
                        _errorMessage.value = "TV sedang dinyalakan, mencoba menghubungkan kembali..."
                        kotlinx.coroutines.delay(4000) // Beri waktu TV untuk inisialisasi hardware awal
                        
                        repeat(4) { attempt ->
                            if (connectionState.value != ConnectionState.CONNECTED) {
                                Log.d(TAG, "Auto-reconnect setelah WOL, percobaan ke-${attempt + 1}")
                                connect() // Panggil fungsi koneksi WebSocket utama
                                kotlinx.coroutines.delay(4000) // Jeda antar percobaan koneksi
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
            val success = com.tvhanan.data.network.AppLauncher.launch(ipAddress, appId)
            Log.d(TAG, "launchApp($appId) success=$success")
        }
    }

    fun closeApp(appId: String) {
        viewModelScope.launch {
            val success = com.tvhanan.data.network.AppLauncher.close(ipAddress, appId)
            Log.d(TAG, "closeApp($appId) success=$success")
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
