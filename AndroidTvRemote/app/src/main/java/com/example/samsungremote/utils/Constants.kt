package com.example.samsungremote.utils

object Constants {
    const val DEFAULT_WSS_PORT = 8002
    const val SSDP_PORT = 1900
    const val SSDP_SEARCH_MESSAGE = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: urn:samsung.com:device:RemoteControlReceiver:1\r\n\r\n"
    const val WAKE_ON_LAN_PORT = 9
    const val WAKE_ON_LAN_PACKET_SIZE = 102
}