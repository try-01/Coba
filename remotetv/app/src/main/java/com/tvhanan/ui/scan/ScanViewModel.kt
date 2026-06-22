package com.tvhanan.ui.scan

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

class ScanViewModel(
    private val discoveryService: TvDiscoveryService = TvDiscoveryService(),
    private val preferences: TvPreferences? = null
) : ViewModel() {

    private val _devices = MutableStateFlow<List<TvDevice>>(emptyList())
    val devices: StateFlow<List<TvDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastIp = MutableStateFlow<String?>(null)
    val lastIp: StateFlow<String?> = _lastIp.asStateFlow()

    init {
        viewModelScope.launch {
            _lastIp.value = preferences?.lastIp?.let { flow -> flow.first() }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null

            try {
                val found = discoveryService.discoverDevices()
                _devices.value = found
                if (found.isEmpty()) {
                    _error.value = "TV tidak ditemukan. Coba koneksi manual."
                }
            } catch (e: Exception) {
                _error.value = "Gagal scan: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun savePreferredDevice(device: TvDevice) {
        viewModelScope.launch {
            preferences?.let {
                it.saveLastIp(device.ipAddress)
                it.saveLastPort(device.port.toString())
                device.macAddress?.let { mac -> it.saveMacAddress(mac) }
            }
        }
    }
}
