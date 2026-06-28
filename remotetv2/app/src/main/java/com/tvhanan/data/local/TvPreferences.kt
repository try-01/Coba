package com.tvhanan.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.GeneralSecurityException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tv_settings")

// Helper untuk menangkap error I/O agar app tidak Force Close
private fun Flow<Preferences>.safeData(): Flow<Preferences> = this.catch { exception ->
    if (exception is IOException) emit(emptyPreferences()) else throw exception
}

// Single source of truth for all preferences - eliminates 4x DataStore subscriptions
private val Context.preferencesFlow: Flow<Preferences> by lazy {
    dataStore.data.safeData().distinctUntilChanged()
}

class TvPreferences(private val context: Context) {

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "tv_secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Failed to create encrypted preferences", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to create encrypted preferences", e)
        }
    }

    companion object {
        private val KEY_LAST_IP = stringPreferencesKey("last_ip")
        private val KEY_LAST_PORT = stringPreferencesKey("last_port")
        private val KEY_MAC_ADDRESS = stringPreferencesKey("mac_address")
        private const val KEY_TOKEN = "token"
        private const val KEY_REMOTE_SIZE = "remote_size"
    }

    // All flows from single source - no duplicate DataStore subscriptions
    val lastIp: Flow<String?> = context.preferencesFlow.map { it[KEY_LAST_IP] }
    val lastPort: Flow<String?> = context.preferencesFlow.map { it[KEY_LAST_PORT] }
    val macAddress: Flow<String?> = context.preferencesFlow.map { it[KEY_MAC_ADDRESS] }
    val remoteSize: Flow<Int> = context.preferencesFlow.map { it.getInt(KEY_REMOTE_SIZE, 1) }

    // Token dibaca dari EncryptedSharedPreferences (bukan Flow karena enkripsi synchronous)
    fun getToken(): String? = encryptedPrefs.getString(KEY_TOKEN, null)

    suspend fun saveLastIp(ip: String) {
        context.dataStore.edit { it[KEY_LAST_IP] = ip }
    }

    suspend fun saveLastPort(port: String) {
        context.dataStore.edit { it[KEY_LAST_PORT] = port }
    }

    // Token disimpan ke EncryptedSharedPreferences
    fun saveToken(token: String) {
        encryptedPrefs.edit().putString(KEY_TOKEN, token).apply()
    }

    suspend fun saveMacAddress(mac: String) {
        context.dataStore.edit { it[KEY_MAC_ADDRESS] = mac }
    }

    fun saveRemoteSize(size: Int) {
        encryptedPrefs.edit().putInt(KEY_REMOTE_SIZE, size).apply()
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
        encryptedPrefs.edit().clear().apply()
    }
}