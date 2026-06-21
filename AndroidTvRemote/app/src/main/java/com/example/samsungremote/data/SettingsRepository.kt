package com.example.samsungremote.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    val tvIp: Flow<String?> = dataStore.data.map { preferences ->
        preferences[stringPreferencesKey("tv_ip")]
    }

    val tvPort: Flow<Int> = dataStore.data.map { preferences ->
        preferences[intPreferencesKey("tv_port")] ?: 8002
    }

    val tvToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[stringPreferencesKey("tv_token")]
    }

    val hapticEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("haptic_enabled")] ?: true
    }

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("keep_screen_on")] ?: false
    }

    suspend fun saveTvConnection(ip: String, port: Int, token: String?) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("tv_ip")] = ip
            preferences[intPreferencesKey("tv_port")] = port
            preferences[stringPreferencesKey("tv_token")] = token ?: ""
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("haptic_enabled")] = enabled
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("keep_screen_on")] = enabled
        }
    }
}