package com.tvhanan

import android.app.Application
import com.tvhanan.util.CrashReporter
import com.tvhanan.util.HapticUtil
import dagger.hilt.android.HiltAndroidApp // DIPERBAIKI: Menambahkan import Hilt yang hilang

@HiltAndroidApp
class TvRemoteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashReporter.initialize(this)
        HapticUtil.init(this)
    }
}