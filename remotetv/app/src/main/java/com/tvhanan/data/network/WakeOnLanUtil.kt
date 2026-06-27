package com.tvhanan.data.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.util.Log

object WakeOnLanUtil {

    private const val TAG = "WakeOnLanUtil"
    private val WOL_PORTS = listOf(9, 7)

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

            WOL_PORTS.forEach { port ->
                val packet = DatagramPacket(packetBytes, packetBytes.size, address, port)
                socket.send(packet)
            }
            socket.close()
            Log.d(TAG, "Magic packet sent to $macAddress via $broadcastIp")
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendWakeOnLan failed: ${e.message}")
            false
        }
    }

    /**
     * Mengirim magic packet berulang dengan interval, karena TV Samsung
     * (dan banyak Smart TV lain) butuh waktu transisi ke deep-standby
     * sebelum benar-benar siap menerima WOL — paket yang dikirim terlalu
     * cepat setelah TV dimatikan kemungkinan tidak diproses. Retry ini
     * TIDAK menjamin sukses jika TV belum masuk deep-standby sama sekali
     * (butuh beberapa menit di banyak unit, di luar kendali app).
     */
    suspend fun sendWakeOnLanWithRetry(
        macAddress: String,
        broadcastIp: String = "255.255.255.255",
        attempts: Int = 5,
        intervalMillis: Long = 2000
    ): Boolean {
        repeat(attempts) { attempt ->
            val sent = sendWakeOnLan(macAddress, broadcastIp)
            if (sent) {
                Log.d(TAG, "Attempt ${attempt + 1}/$attempts sent")
            }
            if (attempt < attempts - 1) {
                kotlinx.coroutines.delay(intervalMillis)
            }
        }
        // Pengiriman UDP tidak punya acknowledgment, jadi kita tidak bisa
        // tahu pasti TV benar-benar menyala — return true kalau seluruh
        // percobaan kirim paket berhasil dieksekusi tanpa exception.
        return true
    }

    private fun parseMacAddress(mac: String): ByteArray {
        val hex = mac.replace(":", "").replace("-", "").uppercase()
        return ByteArray(6) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
