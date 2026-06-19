package com.samsungremote

import android.content.Context
import android.net.wifi.WifiManager
import java.io.BufferedReader
import java.io.FileReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovers Samsung TVs on the LAN using SSDP (UPnP M-SEARCH) and
 * resolves MAC addresses from the kernel ARP cache as a fallback.
 *
 * SSDP multicast group: 239.255.255.250:1900
 */
class SamsungTvDiscovery(private val context: Context) {

    data class DiscoveredTv(
        val ip: String,
        val mac: String?,
        val modelName: String?,
        val locationUrl: String?
    )

    /**
     * Sends an M-SEARCH probe and collects all Samsung RemoteControlReceiver
     * responses within [timeoutMs].
     */
    suspend fun discover(timeoutMs: Long = 5000L): List<DiscoveredTv> =
        withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, DiscoveredTv>() // keyed by IP
            val multicastLock = acquireMulticastLock()

            try {
                DatagramSocket().use { socket ->
                    socket.soTimeout = timeoutMs.toInt()
                    socket.broadcast = true

                    // Probe for Samsung RemoteControlReceiver
                    sendSsdpProbe(socket, "urn:samsung.com:device:RemoteControlReceiver:1")
                    // Also probe for DIAL (many Samsung TVs respond to this too)
                    sendSsdpProbe(socket, "urn:dial-multiscreen-org:device:dial:1")

                    collectResponses(socket, results, timeoutMs)
                }
            } catch (_: Exception) {
                // Non-fatal: fall back to ARP cache
            } finally {
                multicastLock?.release()
            }

            // Resolve any missing MACs from the ARP table
            results.values.map { tv ->
                if (tv.mac == null) {
                    tv.copy(mac = macFromArpCache(tv.ip))
                } else tv
            }
        }

    private fun acquireMulticastLock(): WifiManager.MulticastLock? {
        return try {
            val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifi?.createMulticastLock("SamsungTvDiscovery")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun sendSsdpProbe(socket: DatagramSocket, st: String) {
        val message = (
                "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\n" +
                        "ST: $st\r\n" +
                        "\r\n"
                )
        val packet = DatagramPacket(
            message.toByteArray(Charsets.UTF_8),
            message.length,
            InetAddress.getByName("239.255.255.250"),
            1900
        )
        socket.send(packet)
    }

    private fun collectResponses(
        socket: DatagramSocket,
        results: MutableMap<String, DiscoveredTv>,
        timeoutMs: Long
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                val buf = ByteArray(1536)
                val packet = DatagramPacket(buf, buf.size)
                socket.receive(packet)

                val data = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val ip = packet.address.hostAddress ?: continue

                // Deduplicate by IP
                if (ip in results) continue

                val st = data
                    .lines()
                    .firstOrNull { it.startsWith("ST:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()

                val location = data
                    .lines()
                    .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()

                // Only record if it looks like a Samsung device
                if (st != null && st.contains("samsung", ignoreCase = true)) {
                    results[ip] = DiscoveredTv(
                        ip = ip,
                        mac = null,
                        modelName = null,
                        locationUrl = location
                    )
                }
            } catch (_: SocketTimeoutException) {
                break
            }
        }
    }

    /**
     * Reads the kernel ARP table (`/proc/net/arp`) to resolve a MAC
     * address for [ip]. Returns null if the device has not communicated
     * recently enough to be in the cache.
     */
    private fun macFromArpCache(ip: String): String? {
        return try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.lineSequence()
                    .drop(1)
                    .firstOrNull { line ->
                        line.trimStart().startsWith(ip)
                    }
                    ?.split(Regex("\\s+"))
                    ?.getOrNull(3)
                    ?.takeIf { it.length == 17 } // 00:11:22:33:44:55
            }
        } catch (_: Exception) {
            null
        }
    }
}
