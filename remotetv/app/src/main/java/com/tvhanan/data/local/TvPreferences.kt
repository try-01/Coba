package com.tvhanan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tv_settings")

class TvPreferences(private val context: Context) {

    companion object {
        private val KEY_LAST_IP = stringPreferencesKey("last_ip")
        private val KEY_LAST_PORT = stringPreferencesKey("last_port")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_MAC_ADDRESS = stringPreferencesKey("mac_address")
    }

    val lastIp: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_IP] }
    val lastPort: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_PORT] }
    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val macAddress: Flow<String?> = context.dataStore.data.map { it[KEY_MAC_ADDRESS] }

    suspend fun saveLastIp(ip: String) {
        context.dataStore.edit { it[KEY_LAST_IP] = ip }
    }

    suspend fun saveLastPort(port: String) {
        context.dataStore.edit { it[KEY_LAST_PORT] = port }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun saveMacAddress(mac: String) {
        context.dataStore.edit { it[KEY_MAC_ADDRESS] = mac }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[KEY_TOKEN]
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
