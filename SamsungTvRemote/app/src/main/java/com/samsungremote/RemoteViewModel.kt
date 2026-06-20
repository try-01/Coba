package com.samsungremote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── One-shot events consumed by the Activity ─────────────────

sealed interface ViewModelEvent {
    data object ExitApp : ViewModelEvent
}

// ── Immutable UI state snapshot ──────────────────────────────

data class RemoteUiState(
    val connectionState: TvConnectionState = TvConnectionState.Idle,
    val discoveredTvs: List<SamsungTvDiscovery.DiscoveredTv> = emptyList(),
    val isDiscovering: Boolean = false,
    val showConnectionPanel: Boolean = false,
    val manualIp: String = "",
    val manualMac: String = "",
    val manualToken: String = "",
    val selectedTab: Int = 0,
    val hapticEnabled: Boolean = true,
    val buttonScale: Float = 1.0f,
    val serviceEnabled: Boolean = true,
    val showSettings: Boolean = false,
    val showConnectionSheet: Boolean = false
)

// ── ViewModel ────────────────────────────────────────────────

class RemoteViewModel(
    private val tvManager: SamsungTvManager,
    private val discovery: SamsungTvDiscovery,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private val _events = Channel<ViewModelEvent>(Channel.BUFFERED)
    val viewModelEvents = _events.receiveAsFlow()

    init {
        // Observe connection state from the engine
        viewModelScope.launch {
            tvManager.connectionState.collect { state ->
                val shouldShow = state is TvConnectionState.Idle ||
                        state is TvConnectionState.Disconnected ||
                        state is TvConnectionState.Error
                _uiState.update {
                    it.copy(connectionState = state, showConnectionSheet = shouldShow)
                }
                if (shouldShow) startDiscovery()
            }
        }

        // Observe persisted settings
        viewModelScope.launch {
            settingsDataStore.hapticEnabled.collect { enabled ->
                _uiState.update { it.copy(hapticEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.buttonScale.collect { scale ->
                _uiState.update { it.copy(buttonScale = scale) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.serviceEnabled.collect { enabled ->
                _uiState.update { it.copy(serviceEnabled = enabled) }
            }
        }

        // Pre-fill manual fields from saved credentials
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    manualIp = settingsDataStore.getSavedIp() ?: "",
                    manualMac = settingsDataStore.getSavedMac() ?: ""
                )
            }
        }
    }

    // ── Connection actions ───────────────────────────────────

    fun startDiscovery() {
        if (_uiState.value.isDiscovering) return
        _uiState.update { it.copy(isDiscovering = true) }
        viewModelScope.launch {
            try {
                val tvs = discovery.discover()
                _uiState.update { it.copy(discoveredTvs = tvs, isDiscovering = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isDiscovering = false) }
            }
        }
    }

    fun connectToTv(ip: String, mac: String, token: String? = null) {
        viewModelScope.launch {
            try {
                tvManager.connect(ip, mac, token)
            } catch (_: Exception) {
                // Error surfaced via tvManager.connectionState → TvConnectionState.Error
            }
        }
    }

    fun disconnect() {
        tvManager.disconnect()
    }

    fun setShowConnectionSheet(show: Boolean) {
        _uiState.update { it.copy(showConnectionSheet = show) }
        if (show) startDiscovery()
    }

    // ── Remote control actions ───────────────────────────────

    fun sendKey(key: SamsungRemoteKey) {
        viewModelScope.launch {
            try {
                tvManager.sendKey(key)
            } catch (_: Exception) {
                // Error surfaced via connectionState
            }
        }
    }

    fun sendText(text: String) {
        viewModelScope.launch {
            try {
                tvManager.sendText(text)
            } catch (_: Exception) { }
        }
    }

    fun wakeOnLan(mac: String) {
        viewModelScope.launch {
            try {
                WakeOnLanUtil.send(mac)
            } catch (_: Exception) {
                // WoL is best-effort; ignore send failures silently
            }
        }
    }

    // ── Tab navigation ───────────────────────────────────────

    fun setSelectedTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // ── Manual entry fields ──────────────────────────────────

    fun setManualIp(ip: String) = _uiState.update { it.copy(manualIp = ip) }
    fun setManualMac(mac: String) = _uiState.update { it.copy(manualMac = mac) }
    fun setManualToken(token: String) = _uiState.update { it.copy(manualToken = token) }

    // ── Settings ─────────────────────────────────────────────

    fun setShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setHapticEnabled(enabled) }
    }

    fun setButtonScale(scale: Float) {
        viewModelScope.launch { settingsDataStore.setButtonScale(scale) }
    }

    fun toggleService() {
        val current = _uiState.value.serviceEnabled
        viewModelScope.launch {
            settingsDataStore.setServiceEnabled(!current)
            if (!current) disconnect() else startDiscovery()
        }
    }

    // ── Server / Exit ────────────────────────────────────────

    fun shutdownServer() {
        disconnect()
    }

    fun exitApp() {
        tvManager.shutdown()
        _events.trySend(ViewModelEvent.ExitApp)
    }
}

// ── Factory (manual DI) ──────────────────────────────────────

class RemoteViewModelFactory(
    private val app: SamsungRemoteApp
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RemoteViewModel(
            tvManager = app.tvManager,
            discovery = app.tvDiscovery,
            settingsDataStore = app.settingsDataStore
        ) as T
    }
}
