package com.tvhanan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TvPreferencesTest {

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tvPreferences: TvPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Create a test DataStore
        dataStore = context.createDataStore(name = "test_tv_settings")
        tvPreferences = TvPreferences(context) {
            dataStore
        }
    }

    @Test
    fun `save and retrieve token`() = runBlockingTest {
        val testToken = "test-token-123"

        // Save token
        tvPreferences.saveToken(testToken)

        // Retrieve token
        val retrievedToken = tvPreferences.getToken()

        assertThat(retrievedToken).isEqualTo(testToken)
    }

    @Test
    fun `getToken returns null when not set`() = runBlockingTest {
        val token = tvPreferences.getToken()
        assertThat(token).isNull()
    }

    @Test
    fun `clear removes all values`() = runBlockingTest {
        // Set some values
        tvPreferences.saveToken("test-token")
        tvPreferences.saveLastIp("192.168.1.100")

        // Clear
        tvPreferences.clear()

        // Verify cleared
        assertThat(tvPreferences.getToken()).isNull()
        assertThat(tvPreferences.lastIp.first()).isNull()
    }
}