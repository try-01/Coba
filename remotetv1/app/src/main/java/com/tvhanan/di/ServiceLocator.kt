package com.tvhanan.di

import android.content.Context
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.network.TvDiscoveryService
import com.tvhanan.ui.settings.SettingsViewModel

class ServiceLocator(context: Context) {

    val preferences: TvPreferences by lazy { TvPreferences(context) }
    val discoveryService: TvDiscoveryService by lazy { TvDiscoveryService(context) }

}
