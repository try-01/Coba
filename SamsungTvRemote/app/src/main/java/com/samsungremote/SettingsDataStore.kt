package com.samsungremote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Type-safe wrapper around DataStore<Preferences> for persisting
 * app settings and the last known TV credentials.
 *
 * Reads are exposed as cold [Flow]s that automatically re-emit on
 * change; writes are suspend functions.
 */
class SettingsDataStore(private val dataStore: DataStore<Preferences>) {

    // ── Keys ──────────────────────────────────────────────────

    private companion object {
        val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val KEY_BUTTON_SCALE = floatPreferencesKey("button_scale")
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

        val KEY_SAVED_IP = stringPreferencesKey("saved_ip")
        val KEY_SAVED_MAC = stringPreferencesKey("saved_mac")
        val KEY_SAVED_TOKEN = stringPreferencesKey("saved_token")
        val KEY_REMOTE_NAME = stringPreferencesKey("remote_name")
    }

    // ── Settings flows ────────────────────────────────────────

    val hapticEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_HAPTIC_ENABLED] ?: true }

    val buttonScale: Flow<Float> =
        dataStore.data.map { it[KEY_BUTTON_SCALE] ?: 1.0f }

    val serviceEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_SERVICE_ENABLED] ?: true }

    val darkTheme: Flow<Boolean> =
        dataStore.data.map { it[KEY_DARK_THEME] ?: true }

    // ── Credential flows (one-shot reads also exposed) ────────

    val savedIp: Flow<String?> =
        dataStore.data.map { it[KEY_SAVED_IP] }

    val savedMac: Flow<String?> =
        dataStore.data.map { it[KEY_SAVED_MAC] }

    val savedToken: Flow<String?> =
        dataStore.data.map { it[KEY_SAVED_TOKEN] }

    val remoteName: Flow<String?> =
        dataStore.data.map { it[KEY_REMOTE_NAME] }

    suspend fun getSavedIp(): String? = savedIp.first()
    suspend fun getSavedMac(): String? = savedMac.first()
    suspend fun getSavedToken(): String? = savedToken.first()

    // ── Setting mutations ─────────────────────────────────────

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    }

    suspend fun setButtonScale(scale: Float) {
        dataStore.edit { it[KEY_BUTTON_SCALE] = scale.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SERVICE_ENABLED] = enabled }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    // ── Credential mutations ──────────────────────────────────

    suspend fun saveCredentials(ip: String, mac: String, token: String? = null) {
        dataStore.edit {
            it[KEY_SAVED_IP] = ip
            it[KEY_SAVED_MAC] = mac
            if (token != null) it[KEY_SAVED_TOKEN] = token
        }
    }

    suspend fun clearCredentials() {
        dataStore.edit {
            it.remove(KEY_SAVED_IP)
            it.remove(KEY_SAVED_MAC)
            it.remove(KEY_SAVED_TOKEN)
        }
    }
}
