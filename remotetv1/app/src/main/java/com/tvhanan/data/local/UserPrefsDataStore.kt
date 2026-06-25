package com.tvhanan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.transform.AndThenThen
import androidx.datastore.core.handlers.transform.transform
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.transform.TransformResult
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.MessageLite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import java.io.InputStream
import java.io.OutputStream

// Protobuf serializer for DataStore
private object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            return UserPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}

// Proto-backed DataStore implementation
class UserPrefsDataStore private constructor(
    private val dataStore: DataStore<UserPreferences>
) {
    companion object {
        @Volatile private var INSTANCE: UserPrefsDataStore? = null

        fun getInstance(context: Context): UserPrefsDataStore {
            return INSTANCE ?: synchronized(this) {
                val existingInstance = INSTANCE
                if (existingInstance != null) existingInstance
                else {
                    val prefsDataStore = DataStore.Factory.create(
                        fileName = "user_preferences_pb",
                        serializer = UserPreferencesSerializer
                    ) { context }
                    UserPrefsDataStore(prefsDataStore).also { INSTANCE = it }
                }
            }
        }
    }

    val lastIp: Flow<String?> = dataStore.data.map { it.lastIp }
    val lastPort: Flow<String?> = dataStore.data.map { it.lastPort }
    val token: Flow<String?> = dataStore.data.map { it.token }
    val macAddress: Flow<String?> = dataStore.data.map { it.macAddress }
    val hapticFlow: Flow<Boolean> = dataStore.data.map { it.hapticFeedback }
    val remoteSizeFlow: Flow<Int> = dataStore.data.map { it.remoteSize }
    val showTooltipsFlow: Flow<Boolean> = dataStore.data.map { it.showTooltips }
    val themeFlow: Flow<String> = dataStore.data.map { it.theme }

    suspend fun updateUserPreferences(
        block: UserPreferencesBuilder.() -> Unit
    ) {
        data.updateData { current ->
            val builder = current.toBuilder()
            block(builder)
            builder.build()
        }
    }

    suspend fun clearAll() {
        data.updateData { UserPreferences.getDefaultInstance() }
    }
}