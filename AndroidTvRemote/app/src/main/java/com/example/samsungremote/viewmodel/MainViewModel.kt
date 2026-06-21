package com.example.samsungremote.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.samsungremote.data.SettingsRepository
import com.example.samsungremote.network.SamsungTvManager
import com.example.samsungremote.utils.ConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val tvManager = SamsungTvManager()

    val connectionState = tvManager.connectionState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        ConnectionState.Disconnected
    )

    val hapticEnabled = settingsRepository.hapticEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        true
    )

    fun connect(ip: String, port: Int = 8002) {
        tvManager.connect(ip, port)
    }

    fun sendCommand(command: String) {
        tvManager.sendCommand(command)
    }

    fun disconnect() {
        tvManager.disconnect()
    }
}