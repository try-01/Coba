package com.tvhanan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        val KEY_HAPTIC = booleanPreferencesKey("haptic_enabled")
        val KEY_SCREEN_ON = booleanPreferencesKey("screen_on_enabled")
        val KEY_MESH_BG = booleanPreferencesKey("mesh_background")
        val KEY_REMOTE_SIZE = stringPreferencesKey("remote_size")
    }

    val lastIp: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_IP] }
    val lastPort: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_PORT] }
    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val macAddress: Flow<String?> = context.dataStore.data.map { it[KEY_MAC_ADDRESS] }

    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAPTIC] ?: true }
    val screenOnEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SCREEN_ON] ?: true }
    val meshBackground: Flow<Boolean> = context.dataStore.data.map { it[KEY_MESH_BG] ?: true }
    val remoteSize: Flow<String> = context.dataStore.data.map { it[KEY_REMOTE_SIZE] ?: "fit" }

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

    suspend fun saveHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC] = enabled }
    }

    suspend fun saveScreenOnEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SCREEN_ON] = enabled }
    }

    suspend fun saveMeshBackground(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MESH_BG] = enabled }
    }

    suspend fun saveRemoteSize(size: String) {
        context.dataStore.edit { it[KEY_REMOTE_SIZE] = size }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[KEY_TOKEN]
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
