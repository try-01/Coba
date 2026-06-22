package com.tvhanan.data.network

import com.tvhanan.domain.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException

class TvDiscoveryService {

    companion object {
        private const val SSDP_ADDR = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SSDP_TIMEOUT = 3000L
        private const val SCAN_TIMEOUT = 300
        private const val TARGET_PORT = 8001
    }

    suspend fun discoverDevices(): List<TvDevice> = withContext(Dispatchers.IO) {
        val ssdpResults = discoverSSDP()
        if (ssdpResults.isNotEmpty()) return@withContext ssdpResults

        val subnet = getLocalIpPrefix() ?: return@withContext emptyList()
        scanSubnet(subnet)
    }

    private suspend fun discoverSSDP(): List<TvDevice> {
        return withContext(Dispatchers.IO) {
            val socket = DatagramSocket()
            socket.soTimeout = SSDP_TIMEOUT.toInt()

            val ssdpRequest = buildString {
                append("M-SEARCH * HTTP/1.1\r\n")
                append("HOST: $SSDP_ADDR:$SSDP_PORT\r\n")
                append("MAN: \"ssdp:discover\"\r\n")
                append("ST: urn:samsung.com:device:RemoteControlReceiver:1\r\n")
                append("MX: 2\r\n")
                append("\r\n")
            }

            val sendPacket = DatagramPacket(
                ssdpRequest.toByteArray(),
                ssdpRequest.length,
                InetAddress.getByName(SSDP_ADDR),
                SSDP_PORT
            )
            socket.send(sendPacket)

            val results = mutableListOf<TvDevice>()
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < SSDP_TIMEOUT) {
                try {
                    val buffer = ByteArray(1024)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    val response = String(packet.data, 0, packet.length)
                    val ip = parseLocationIp(response)
                    if (ip != null) {
                        results.add(TvDevice(ipAddress = ip, name = "Samsung TV"))
                    }
                } catch (_: SocketTimeoutException) {
                    break
                } catch (_: Exception) {
                    continue
                }
            }

            socket.close()
            results.distinctBy { it.ipAddress }
        }
    }

    private suspend fun scanSubnet(prefix: String): List<TvDevice> = coroutineScope {
        val results = (1..254).map { octet ->
            async {
                val ip = "$prefix.$octet"
                if (isPortOpen(ip, TARGET_PORT)) {
                    TvDevice(ipAddress = ip)
                } else if (isPortOpen(ip, 8002)) {
                    TvDevice(ipAddress = ip, port = 8002)
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()

        results
    }

    private fun isPortOpen(ip: String, port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), SCAN_TIMEOUT)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalIpPrefix(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        return ip.substringBeforeLast(".")
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLocationIp(response: String): String? {
        val locationHeader = response.lines().firstOrNull {
            it.startsWith("LOCATION:", ignoreCase = true) ||
                it.startsWith("location:", ignoreCase = true)
        } ?: return null

        val url = locationHeader.substringAfter(":").trim()
        return try {
            val host = InetAddress.getByName(
                url.removePrefix("http://").removePrefix("https://").substringBefore("/")
                    .substringBefore(":")
            )
            host.hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
