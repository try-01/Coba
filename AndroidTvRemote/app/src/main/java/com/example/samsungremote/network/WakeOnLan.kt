package com.example.samsungremote.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import com.example.samsungremote.utils.Constants

class WakeOnLan {
    suspend fun sendMagicPacket(macAddress: String, ip: String? = null) = withContext(Dispatchers.IO) {
        val macBytes = macAddress.replace("(.{2})".toRegex(), "$1 ").trim().split(" ").map { it.toInt(16).toByte() }
        val bytes = ByteArray(Constants.WAKE_ON_LAN_PACKET_SIZE) {
            if (it < 6) 0xFF.toByte()
            else macBytes[(it - 6) % 6]
        }

        val address = ip?.let { InetAddress.getByName(it) } ?: InetAddress.getByName("255.255.255.255")
        val packet = DatagramPacket(bytes, bytes.size, address, Constants.WAKE_ON_LAN_PORT)

        DatagramSocket().use { socket ->
            socket.send(packet)
        }
    }
}