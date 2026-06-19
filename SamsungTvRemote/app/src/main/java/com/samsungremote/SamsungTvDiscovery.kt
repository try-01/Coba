package com.samsungremote

import android.content.Context
import android.net.wifi.WifiManager
import java.io.BufferedReader
import java.io.FileReader
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovers Samsung TVs on the LAN via SSDP (UPnP M-SEARCH).
 *
 * Sends probes for several Samsung-specific service types and a
 * catch-all `ssdp:all`.  Responses are collected until the timeout
 * expires.  MAC addresses are resolved from the kernel ARP cache.
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

    private val targetStList = listOf(
        "urn:samsung.com:device:RemoteControlReceiver:1",
        "urn:dial-multiscreen-org:device:dial:1",
        "ssdp:all",
        "upnp:rootdevice"
    )

    /**
     * Sends M-SEARCH probes and collects Samsung TV responses.
     * Uses [MulticastSocket] joined to the SSDP group so Android's
     * network stack delivers multicast replies reliably.
     */
    suspend fun discover(timeoutMs: Long = 6000L): List<DiscoveredTv> =
        withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, DiscoveredTv>()
            val multicastLock = acquireMulticastLock()
            val group = InetAddress.getByName("239.255.255.250")

            try {
                MulticastSocket().use { socket ->
                    socket.soTimeout = timeoutMs.toInt()
                    socket.joinGroup(group)
                    socket.timeToLive = 4
                    socket.reuseAddress = true

                    for (st in targetStList) {
                        sendSsdpProbe(socket, group, st)
                    }

                    collectResponses(socket, results, timeoutMs)

                    try { socket.leaveGroup(group) } catch (_: Exception) { }
                }
            } catch (_: Exception) {
                // Non-fatal — fall back to ARP-based scan
            } finally {
                multicastLock?.release()
            }

            results.values.map { tv ->
                if (tv.mac == null) tv.copy(mac = macFromArpCache(tv.ip)) else tv
            }
        }

    /**
     * Fallback scan that tries to resolve MACs from the ARP cache
     * without sending any network probes.  Useful when SSDP is
     * blocked (e.g. guest Wi-Fi, VPN).
     */
    suspend fun scanArpOnly(): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiscoveredTv>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.lineSequence().drop(1).forEach { line ->
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 4 && parts[3].length == 17 && parts[3] != "00:00:00:00:00:00") {
                        val ip = parts[0]
                        val mac = parts[3]
                        results.add(DiscoveredTv(ip = ip, mac = mac, modelName = null, locationUrl = null))
                    }
                }
            }
        } catch (_: Exception) { }
        results
    }

    // ── Private helpers ───────────────────────────────────────

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

    private fun sendSsdpProbe(
        socket: MulticastSocket,
        group: InetAddress,
        st: String
    ) {
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
            group,
            1900
        )
        socket.send(packet)
    }

    private fun collectResponses(
        socket: MulticastSocket,
        results: MutableMap<String, DiscoveredTv>,
        timeoutMs: Long
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                val buf = ByteArray(2048)
                val packet = DatagramPacket(buf, buf.size)
                socket.receive(packet)

                val data = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val ip = packet.address?.hostAddress ?: continue
                if (ip in results) continue

                val headers = data.lines()
                val st = headers
                    .firstOrNull { it.startsWith("ST:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()

                val location = headers
                    .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()

                val server = headers
                    .firstOrNull { it.startsWith("SERVER:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()

                val usn = headers
                    .firstOrNull { it.startsWith("USN:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()

                // Accept any device with Samsung in ST, SERVER, or USN
                val isSamsung = listOfNotNull(st, server, usn)
                    .any { it.contains("samsung", ignoreCase = true) }

                if (isSamsung) {
                    results[ip] = DiscoveredTv(
                        ip = ip,
                        mac = null,
                        modelName = server?.let { extractModel(it) },
                        locationUrl = location
                    )
                }
            } catch (_: SocketTimeoutException) {
                break
            }
        }
    }

    private fun macFromArpCache(ip: String): String? {
        return try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.lineSequence()
                    .drop(1)
                    .firstOrNull { it.trimStart().startsWith(ip) }
                    ?.split(Regex("\\s+"))
                    ?.getOrNull(3)
                    ?.takeIf { it.length == 17 }
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Crude model-name extraction from the SERVER header. */
    private fun extractModel(server: String): String? {
        val candidates = listOf("UN", "UE", "QN", "QE", "LS", "UA")
        return candidates.firstOrNull { server.contains(it) }
            ?.let { model ->
                val idx = server.indexOf(model)
                if (idx >= 0) server.substring(idx, (idx + 12).coerceAtMost(server.length))
                    .takeUnless { it.isBlank() } else null
            }
    }
}
