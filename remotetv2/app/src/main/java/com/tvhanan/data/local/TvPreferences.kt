package com.tvhanan.data.local

import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.catch
import java.io.IOException
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tv_settings")

class TvPreferences(private val context: Context) {

    private val Flow<Preferences>.safeData: Flow<Preferences>
        get() = this.catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    /** EncryptedSharedPreferences khusus untuk token pairing */
    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "tv_token_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private val KEY_LAST_IP = stringPreferencesKey("last_ip")
        private val KEY_LAST_PORT = stringPreferencesKey("last_port")
        private val KEY_MAC_ADDRESS = stringPreferencesKey("mac_address")
        private const val KEY_TOKEN_SECURE = "token"
    }

    val lastIp: Flow<String?> = context.dataStore.data.safeData.map { it[KEY_LAST_IP] }
    val lastPort: Flow<String?> = context.dataStore.data.safeData.map { it[KEY_LAST_PORT] }
    val macAddress: Flow<String?> = context.dataStore.data.safeData.map { it[KEY_MAC_ADDRESS] }

    suspend fun saveLastIp(ip: String) {
        context.dataStore.edit { it[KEY_LAST_IP] = ip }
    }

    suspend fun saveLastPort(port: String) {
        context.dataStore.edit { it[KEY_LAST_PORT] = port }
    }

    suspend fun saveToken(token: String) {
        securePrefs.edit().putString(KEY_TOKEN_SECURE, token).apply()
    }

    suspend fun saveMacAddress(mac: String) {
        context.dataStore.edit { it[KEY_MAC_ADDRESS] = mac }
    }

    suspend fun getToken(): String? {
        return securePrefs.getString(KEY_TOKEN_SECURE, null)
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
        securePrefs.edit().clear().apply()
    }
}
