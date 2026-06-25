package com.tvhanan.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvDiscoveryService
import com.tvhanan.domain.model.ConnectionActionState
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: TvPreferences,
    private val discoveryService: TvDiscoveryService
) : ViewModel() {

    private val _uiPreferences = MutableStateFlow(UiPrefs())
    val uiPreferences: StateFlow<UiPrefs> = _uiPreferences.asStateFlow()

    private val _tvDevice = MutableStateFlow<**(device: TvDevice?)?**()
    val tvDevice: StateFlow<TvDevice?> = _tvDevice.asStateFlow()

    private val _actionState = MutableStateFlow(ConnectionActionState.Idle)
    val actionState: StateFlow<ConnectionActionState> = _actionState.asStateFlow()

    init {
        viewModelScope.launch {
            val ip = preferences.lastIp.first()
            val port = preferences.lastPort.first()?.toIntOrNull() ?: 8001
            val mac = preferences.macAddress.first()
            _tvDevice.value = if (ip != null) TvDevice(ip, "Saved TV", mac, port) else null
        }
    }

    fun setActiveDevice(
        ipAddress: String,
        port: Int,
        macAddress: String? = null,
        token: String? = null,
        isConnected: Boolean = false
    ) {
        viewModelScope.launch {
            preferences.saveLastIp(ipAddress)
            preferences.saveLastPort(port.toString())
            macAddress?.let { preferences.saveMacAddress(it) }
            token?.let { preferences.saveToken(it) }

            _tvDevice.value = TvDevice(ipAddress, "Saved TV", macAddress, port)
            // Note: UI may want to react to connection status; expose via separate flow if needed
        }
    }

    fun reconnect() {
        val device = _tvDevice.value ?: return
        viewModelScope.launch {
            _actionState.value = ConnectionActionState.Loading
            try {
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

    companion object {
        private const val TAG = "TvHanan"
    }

    data class UiPrefs(
        val remoteSize: RemoteSize = RemoteSize.MEDIUM,
        val hapticEnabled: Boolean = true,
        val showTooltips: Boolean = true
    )
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

    private val _isActuallyConnected = MutableStateFlow(false)
    val isActuallyConnected: StateFlow<Boolean> = _isActuallyConnected.asStateFlow()

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
    fun setActiveDevice(ipAddress: String, port: Int, macAddress: String?, token: String? = null, isConnected: Boolean = false) {
        _tvDevice.value = TvDevice(ipAddress = ipAddress, port = port, macAddress = macAddress, token = token)
        _isActuallyConnected.value = isConnected
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
