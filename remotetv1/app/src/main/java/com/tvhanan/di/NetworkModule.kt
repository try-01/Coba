package com.tvhanan.di

import android.content.Context
import com.tvhanan.data.local.TvPreferences
import com.tvhanan.data.local.UserPrefsDataStore
import com.tvhanan.data.network.TvDiscoveryService // DIPERBAIKI: Import package yang benar (network, bukan local)
import com.tvhanan.data.network.TvWebSocketClient
import com.tvhanan.ui.settings.SettingsViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext // DIPERBAIKI: Menambahkan import @ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTvDiscoveryService(@ApplicationContext ctx: Context): TvDiscoveryService {
        return TvDiscoveryService(ctx)
    }

    @Provides
    @Singleton
    fun provideTvWebSocketClient(): TvWebSocketClient {
        return TvWebSocketClient()
    }

    @Provides
    @Singleton
    fun provideTvPreferences(@ApplicationContext ctx: Context): TvPreferences {
        return TvPreferences(ctx)
    }

    @Provides
    @Singleton
    fun provideUserPrefsDataStore(@ApplicationContext ctx: Context): UserPrefsDataStore {
        return UserPrefsDataStore.getInstance(ctx)
    }

    // CATATAN: Fungsi @Binds abstrak sebelumnya dihapus karena ilegal di dalam 'object' 
    // dan sudah sepenuhnya terwakili oleh fungsi @Provides di atas.

    @Provides
    @Singleton
    fun provideSettingsViewModel(
        preferences: TvPreferences,
        discoveryService: TvDiscoveryService
    ): SettingsViewModel {
        return SettingsViewModel(preferences, discoveryService)
    }
}