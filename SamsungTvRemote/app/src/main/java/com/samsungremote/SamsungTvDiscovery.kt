package com.samsungremote

import android.content.Context
import android.net.wifi.WifiManager
import java.io.BufferedReader
import java.io.FileReader
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SamsungTvDiscovery(
    private val context: Context,
    private val logger: AppLogger
) {

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

    private val probePorts = listOf(8001, 8002)

    suspend fun discover(timeoutMs: Long = 6000L): List<DiscoveredTv> =
        withContext(Dispatchers.IO) {
            logger.i("Disc", "Starting discovery (timeout=${timeoutMs}ms)")

            // 1) Try SSDP
            val ssdpResults = ssdpDiscover(timeoutMs)
            if (ssdpResults.isNotEmpty()) {
                logger.i("Disc", "SSDP found ${ssdpResults.size} TV(s)")
                return@withContext ssdpResults
            }

            // 2) Fallback: ARP cache
            val arpResults = scanArpOnly()
            if (arpResults.isNotEmpty()) {
                logger.i("Disc", "ARP found ${arpResults.size} device(s) — probing ports")
                val confirmed = arpResults.filter { isTvPortOpen(it.ip) }
                if (confirmed.isNotEmpty()) {
                    return@withContext confirmed.map { it.copy(modelName = "Samsung TV (ARP)") }
                }
            }

            // 3) Last resort: probe subnet ports
            logger.i("Disc", "SSDP + ARP found nothing — probing subnet ports")
            val probed = probeSubnet()
            logger.i("Disc", "Probe found ${probed.size} TV(s)")
            return@withContext probed
        }

    // ── SSDP (MulticastSocket, fixed for Android 14+) ──────────

    private suspend fun ssdpDiscover(timeoutMs: Long): List<DiscoveredTv> =
        withContext(Dispatchers.IO) {
            logger.i("Disc", "SSDP discovery (timeout=${timeoutMs}ms)")
            val results = mutableMapOf<String, DiscoveredTv>()
            val multicastLock = acquireMulticastLock()
            val group = InetAddress.getByName("239.255.255.250")
            val wifiIface = findWifiInterface()

            if (wifiIface == null) {
                logger.w("Disc", "No WiFi interface found — skipping SSDP")
                multicastLock?.release()
                return@withContext emptyList()
            }

            try {
                MulticastSocket().use { socket ->
                    socket.soTimeout = timeoutMs.toInt()
                    socket.timeToLive = 4
                    socket.reuseAddress = true
                    socket.networkInterface = wifiIface
                    val groupAddr = InetSocketAddress(group, 1900)
                    socket.joinGroup(groupAddr, wifiIface)

                    for (st in targetStList) {
                        sendSsdpProbe(socket, group, st)
                    }

                    collectResponses(socket, results, timeoutMs)

                    try { socket.leaveGroup(groupAddr, wifiIface) } catch (_: Exception) { }
                }
            } catch (e: Exception) {
                logger.w("Disc", "SSDP failed: ${e.localizedMessage ?: e::class.simpleName}")
            } finally {
                multicastLock?.release()
            }

            results.values.map { tv ->
                if (tv.mac == null) tv.copy(mac = macFromArpCache(tv.ip)) else tv
            }
        }

    // ── Subnet port probing ────────────────────────────────────

    private suspend fun probeSubnet(): List<DiscoveredTv> =
        withContext(Dispatchers.IO) {
            val baseIp = getLocalIpPrefix() ?: return@withContext emptyList()
            logger.i("Disc", "Probing subnet $baseIp.0/24 on ports ${probePorts.joinToString(",")}")

            val found = mutableListOf<DiscoveredTv>()
            for (lastOctet in 1..254) {
                val ip = "$baseIp.$lastOctet"
                for (port in probePorts) {
                    if (isPortOpen(ip, port, 150)) {
                        logger.i("Disc", "Port $port open on $ip")
                        val mac = macFromArpCache(ip)
                        found.add(DiscoveredTv(ip = ip, mac = mac, modelName = null, locationUrl = null))
                        break
                    }
                }
            }
            found
        }

    private fun isTvPortOpen(ip: String): Boolean =
        probePorts.any { isPortOpen(ip, it, 200) }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { sock ->
                sock.connect(InetSocketAddress(ip, port), timeoutMs)
                sock.close()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    // ── ARP-only scan ──────────────────────────────────────────

    suspend fun scanArpOnly(): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        logger.i("Disc", "ARP-only scan")
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
            logger.i("Disc", "ARP scan found ${results.size} device(s)")
        } catch (e: Exception) {
            logger.w("Disc", "ARP scan failed: ${e.localizedMessage}")
        }
        results
    }

    // ── Network helpers ────────────────────────────────────────

    private fun getLocalIpPrefix(): String? {
        return try {
            val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wifi?.dhcpInfo ?: return null
            val ip = dhcp.ipAddress
            val mask = dhcp.netmask
            if (ip == 0 || mask == 0) return null
            val network = ip and mask
            val a = network shr 24 and 0xFF
            val b = network shr 16 and 0xFF
            val c = network shr 8 and 0xFF
            "$a.$b.$c"
        } catch (_: Exception) {
            null
        }
    }

    private fun findWifiInterface(): NetworkInterface? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.find { it.isUp && !it.isLoopback && it.name.startsWith("wlan") }
        } catch (_: Exception) {
            null
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

    // ── SSDP helpers ───────────────────────────────────────────

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

                val isSamsung = listOfNotNull(st, server, usn)
                    .any { it.contains("samsung", ignoreCase = true) }

                if (isSamsung) {
                    val model = server?.let { extractModel(it) }
                    logger.d("Disc", "SSDP response from $ip (model=$model)")
                    results[ip] = DiscoveredTv(
                        ip = ip,
                        mac = null,
                        modelName = model,
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
            val mac = BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.lineSequence()
                    .drop(1)
                    .firstOrNull { it.trimStart().startsWith(ip) }
                    ?.split(Regex("\\s+"))
                    ?.getOrNull(3)
                    ?.takeIf { it.length == 17 }
            }
            if (mac != null) logger.d("Disc", "ARP cache resolved $ip → $mac")
            mac
        } catch (e: Exception) {
            logger.w("Disc", "ARP read failed for $ip: ${e.localizedMessage}")
            null
        }
    }

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

