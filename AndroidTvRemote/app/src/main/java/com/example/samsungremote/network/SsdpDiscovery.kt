package com.example.samsungremote.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

import com.example.samsungremote.utils.Constants

class SsdpDiscovery {
    suspend fun discoverTvs(): List<String> = withContext(Dispatchers.IO) {
        val discoveredIps = mutableListOf<String>()
        MulticastSocket().use { socket ->
            socket.joinGroup(InetAddress.getByName("239.255.255.250"))
            socket.socketTimeout = 5000

            val message = Constants.SSDP_SEARCH_MESSAGE.toByteArray()
            val packet = DatagramPacket(
                message,
                message.size,
                InetAddress.getByName("239.255.255.250"),
                Constants.SSDP_PORT
            )
            socket.send(packet)

            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    if (response.contains("Samsung")) {
                        val ip = receivePacket.address.hostAddress
                        if (ip != null && !discoveredIps.contains(ip)) {
                            discoveredIps.add(ip)
                        }
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
        discoveredIps
    }
}