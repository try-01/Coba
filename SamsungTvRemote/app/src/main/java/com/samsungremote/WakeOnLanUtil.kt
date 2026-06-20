package com.samsungremote

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Sends a Wake-on-LAN magic packet over UDP to wake a sleeping TV.
 *
 * The packet consists of 6 bytes of 0xFF followed by the target MAC
 * address repeated 16 times (102 bytes total).
 */
object WakeOnLanUtil {

    private const val MAGIC_PACKET_LENGTH = 102
    private const val WOl_PORT = 9

    fun send(macAddress: String, logger: AppLogger? = null) {
        val cleaned = macAddress
            .replace(":", "")
            .replace("-", "")
            .trim()

        if (cleaned.length != 12) {
            val msg = "Invalid MAC address: expected 12 hex digits, got ${cleaned.length}"
            logger?.e("WoL", msg)
            throw IllegalArgumentException(msg)
        }

        val macBytes = cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val packet = ByteArray(MAGIC_PACKET_LENGTH)

        for (i in 0 until 6) {
            packet[i] = 0xFF.toByte()
        }
        for (i in 0 until 16) {
            System.arraycopy(macBytes, 0, packet, 6 + i * 6, 6)
        }

        logger?.d("WoL", "Sending magic packet to ${cleaned.uppercase()}")
        DatagramSocket().use { socket ->
            socket.broadcast = true
            val address = InetAddress.getByName("255.255.255.255")
            val dp = DatagramPacket(packet, packet.size, address, WOl_PORT)
            socket.send(dp)
        }
        logger?.i("WoL", "Magic packet sent to $macAddress")
    }
}
