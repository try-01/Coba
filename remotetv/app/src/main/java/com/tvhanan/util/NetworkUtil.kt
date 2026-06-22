package com.tvhanan.util

import android.content.Context
import android.net.wifi.WifiManager

object NetworkUtil {

    fun getWifiIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null

        val connectionInfo = wifiManager.connectionInfo ?: return null
        val ipInt = connectionInfo.ipAddress
        if (ipInt == 0) return null

        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xFF,
            ipInt shr 8 and 0xFF,
            ipInt shr 16 and 0xFF,
            ipInt shr 24 and 0xFF
        )
    }
}
