package com.example.samsungremote.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService

class SamsungRemoteService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun stopService() {
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? = null
}