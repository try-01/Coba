package com.tvhanan.di

import android.content.Context
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvDiscoveryService
import com.tvhanan.ui.settings.SettingsViewModel

class ServiceLocator(context: Context) {

    val preferences: TvPreferences by lazy { TvPreferences(context) }
    val discoveryService: TvDiscoveryService by lazy { TvDiscoveryService(context) }

    /**
     * SettingsViewModel disimpan sebagai singleton (bukan dibuat ulang per
     * navigasi composable seperti ViewModel screen lain) supaya preferensi
     * tampilan (remoteSize, hapticEnabled, dst) tetap konsisten dan bisa
     * dibaca bersama oleh RemoteScreen maupun SettingsScreen tanpa perlu
     * disimpan ulang lewat DataStore/SavedStateHandle.
     */
    val settingsViewModel: SettingsViewModel by lazy {
        SettingsViewModel(preferences = preferences, discoveryService = discoveryService)
    }
}
