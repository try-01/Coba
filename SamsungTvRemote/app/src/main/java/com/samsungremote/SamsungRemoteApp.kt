package com.samsungremote

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "samsung_remote_prefs"
)

class SamsungRemoteApp : Application() {
    lateinit var logger: AppLogger
        private set

    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(dataStore) }
    val tvManager: SamsungTvManager by lazy { SamsungTvManager(settingsDataStore, logger) }
    val tvDiscovery: SamsungTvDiscovery by lazy { SamsungTvDiscovery(this, logger) }

    override fun onCreate() {
        super.onCreate()
        logger = AppLogger.create(this)
        logger.i("App", "App started — logs: ${filesDir}/logs/")
    }
}
