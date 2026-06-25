package com.tvhanan.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvDiscoveryService
import com.tvhanan.domain.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Status sebuah aksi koneksi (reconnect/scan) yang ditampilkan sbg modal di SettingsScreen. */
sealed interface ConnectionActionState {
    data object Idle : ConnectionActionState
    data object Loading : ConnectionActionState
    data class ReconnectSuccess(val device: TvDevice) : ConnectionActionState
    data class ScanResult(val devices: List<TvDevice>) : ConnectionActionState
    data class Failed(val message: String) : ConnectionActionState
}

/**
 * Preferensi tampilan remote. Catatan implementasi: `remoteSize` saat ini
 * disimpan di state SettingsViewModel tapi BELUM dikonsumsi oleh
 * RemoteScreen — untuk efek visual sungguhan (tombol membesar/mengecil),
 * RemoteScreen perlu membaca preferensi ini (mis. lewat ServiceLocator
 * yang menyediakan SettingsViewModel sbg singleton, atau lewat
 * SavedStateHandle/DataStore bersama) dan mengalikan ukuran dp tombol
 * dengan faktor skala sesuai RemoteSize. Ditandai jelas di sini supaya
 * tidak disangka sudah berfungsi penuh.
 */
data class RemoteUiPreferences(
    val hapticEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val meshBackgroundEnabled: Boolean = true,
    val remoteSize: RemoteSize = RemoteSize.FIT
)

enum class RemoteSize(val scaleFactor: Float) {
    COMPACT(0.86f),
    FIT(1.0f),
    LARGE(1.14f)
}

class SettingsViewModel(
    private val preferences: TvPreferences,
    private val discoveryService: TvDiscoveryService
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _tvDevice = MutableStateFlow<TvDevice?>(null)
    val tvDevice: StateFlow<TvDevice?> = _tvDevice.asStateFlow()

    private val _uiPreferences = MutableStateFlow(RemoteUiPreferences())
    val uiPreferences: StateFlow<RemoteUiPreferences> = _uiPreferences.asStateFlow()

    private val _actionState = MutableStateFlow<ConnectionActionState>(ConnectionActionState.Idle)
    val actionState: StateFlow<ConnectionActionState> = _actionState.asStateFlow()

    init {
        loadCurrentDevice()
    }

    private fun loadCurrentDevice() {
        viewModelScope.launch {
            val ip = preferences.lastIp.first()
            val port = preferences.lastPort.first()?.toIntOrNull() ?: 8001
            val mac = preferences.macAddress.first()
            if (ip != null) {
                _tvDevice.value = TvDevice(ipAddress = ip, port = port, macAddress = mac)
            }
        }
    }

/** Dipanggil RemoteScreen begitu IP/port/mac aktif diketahui, supaya
     * TvInfoCard di Settings langsung akurat tanpa menunggu DataStore. */
    fun setActiveDevice(ipAddress: String, port: Int, macAddress: String?) {
        _tvDevice.value = TvDevice(ipAddress = ipAddress, port = port, macAddress = macAddress)
    }

    fun setHapticEnabled(enabled: Boolean) {
        _uiPreferences.value = _uiPreferences.value.copy(hapticEnabled = enabled)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _uiPreferences.value = _uiPreferences.value.copy(keepScreenOn = enabled)
    }

    fun setMeshBackgroundEnabled(enabled: Boolean) {
        _uiPreferences.value = _uiPreferences.value.copy(meshBackgroundEnabled = enabled)
    }

    fun setRemoteSize(size: RemoteSize) {
        _uiPreferences.value = _uiPreferences.value.copy(remoteSize = size)
    }

    fun reconnect() {
        val device = _tvDevice.value ?: return
        viewModelScope.launch {
            _actionState.value = ConnectionActionState.Loading
            try {
                // Reconnect WebSocket sesungguhnya dilakukan oleh RemoteViewModel
                // saat RemoteScreen dibuka kembali. Di sini kita hanya verifikasi
                // host terjangkau di jaringan, supaya modal Settings bisa memberi
                // feedback cepat sebelum user kembali ke RemoteScreen.
                val reachable = discoveryService.isHostReachable(device.ipAddress, device.port)
                _actionState.value = if (reachable) {
                    ConnectionActionState.ReconnectSuccess(device)
                } else {
                    ConnectionActionState.Failed("TV tidak merespons di ${device.ipAddress}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect probe failed", e)
                _actionState.value = ConnectionActionState.Failed(e.message ?: "Gagal menghubungkan ulang")
            }
        }
    }

    fun scanForOtherTvs() {
        viewModelScope.launch {
            _actionState.value = ConnectionActionState.Loading
            try {
                val devices = discoveryService.discoverDevices()
                _actionState.value = ConnectionActionState.ScanResult(devices)
            } catch (e: Exception) {
                Log.e(TAG, "Scan failed", e)
                _actionState.value = ConnectionActionState.Failed(e.message ?: "Gagal memindai jaringan")
            }
        }
    }

    fun forgetTv() {
        viewModelScope.launch {
            preferences.clear()
            _tvDevice.value = null
            _actionState.value = ConnectionActionState.Idle
        }
    }

    fun resetActionState() {
        _actionState.value = ConnectionActionState.Idle
    }
}
