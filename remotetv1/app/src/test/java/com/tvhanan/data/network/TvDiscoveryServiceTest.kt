package com.tvhanan.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tvhanan.domain.model.TvDevice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RebolectricTestRunner::class)
@Config(sdk = [30])
class TvDiscoveryServiceTest {

    private lateinit var context: Context
    private lateinit var service: TvDiscoveryService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = TvDiscoveryService(context)
    }

    @Test
    fun `isOpen returns false for unreachable port`() = runBlockingTest {
        // Using localhost port that is unlikely to be open
        val result = service.isPortOpen("127.0.0.1", 12345)
        assertThat(result).isFalse()
    }

    @Test
    fun `discoverDevices returns list`() = runBlockingTest {
        // This will execute real discovery; may be empty depending on environment.
        // We just ensure it doesn't throw.
        val result = service.discoverDevices()
        assertThat(result).isInstanceOf(List::class.java)
        // Each element if any should be TvDevice
        result.forEach { assertThat(it).isInstanceOf(TvDevice::class.java) }
    }
}