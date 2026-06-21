package com.example.samsungremote.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.samsungremote.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    val keepScreenOn = settingsRepository.keepScreenOn.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        false
    )

    fun saveTvConnection(ip: String, port: Int, token: String?) {
        viewModelScope.launch {
            settingsRepository.saveTvConnection(ip, port, token)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
        }
    }
}