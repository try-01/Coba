package com.example.samsungremote.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService

class DiscoveryService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun createNotification(): android.app.Notification {
        return android.app.Notification.Builder(this, "discovery_channel")
            .setContentTitle("Samsung TV Discovery")
            .setContentText("Searching for Samsung TVs...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? = null
}