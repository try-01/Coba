package com.tvhanan.data.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLanUtil {

    private const val DEFAULT_PORT = 9

    fun sendWakeOnLan(macAddress: String, broadcastIp: String = "255.255.255.255"): Boolean {
        return try {
            val macBytes = parseMacAddress(macAddress)
            val packetBytes = ByteArray(6 + 16 * macBytes.size)

            for (i in 0 until 6) {
                packetBytes[i] = 0xFF.toByte()
            }
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, packetBytes, 6 + i * macBytes.size, macBytes.size)
            }

            val address = InetAddress.getByName(broadcastIp)
            val socket = DatagramSocket()
            socket.broadcast = true

            val packet = DatagramPacket(packetBytes, packetBytes.size, address, DEFAULT_PORT)
            socket.send(packet)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseMacAddress(mac: String): ByteArray {
        val hex = mac.replace(":", "").replace("-", "").uppercase()
        return ByteArray(6) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
