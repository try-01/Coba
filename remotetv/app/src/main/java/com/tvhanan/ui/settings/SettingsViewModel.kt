package com.tvhanan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.util.HapticUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SettingsEvent {
    data object NavigateToScan : SettingsEvent()
    data object NavigateToManual : SettingsEvent()
    data object NavigateToScanOnForget : SettingsEvent()
    data object ExitApp : SettingsEvent()
    data object ShowFeedback : SettingsEvent()
}

class SettingsViewModel(
    private val preferences: TvPreferences
) : ViewModel() {

    val hapticEnabled: StateFlow<Boolean> = preferences.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val screenOnEnabled: StateFlow<Boolean> = preferences.screenOnEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val meshBackground: StateFlow<Boolean> = preferences.meshBackground
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val remoteSize: StateFlow<String> = preferences.remoteSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, "fit")

    private val _events = MutableStateFlow<SettingsEvent?>(null)
    val events: StateFlow<SettingsEvent?> = _events.asStateFlow()

    fun onEventHandled() {
        _events.value = null
    }

    fun toggleHaptic(enabled: Boolean) {
        HapticUtil.setEnabled(enabled)
        viewModelScope.launch {
            preferences.saveHapticEnabled(enabled)
        }
    }

    fun toggleScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveScreenOnEnabled(enabled)
        }
    }

    fun toggleMeshBackground(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveMeshBackground(enabled)
        }
    }

    fun setRemoteSize(size: String) {
        viewModelScope.launch {
            preferences.saveRemoteSize(size)
        }
    }

    fun forgetTv() {
        viewModelScope.launch {
            preferences.clear()
            _events.value = SettingsEvent.NavigateToScanOnForget
        }
    }

    fun exitApp() {
        _events.value = SettingsEvent.ExitApp
    }

    fun feedback() {
        _events.value = SettingsEvent.ShowFeedback
    }

    fun scanTv() {
        _events.value = SettingsEvent.NavigateToScan
    }

    fun manualConnect() {
        _events.value = SettingsEvent.NavigateToManual
    }
}
