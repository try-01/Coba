package com.tvhanan

import android.app.Application
import com.tvhanan.util.CrashReporter
import com.tvhanan.util.HapticUtil

@HiltAndroidApp
class TvRemoteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashReporter.initialize(this)
        HapticUtil.init(this)
    }
}
