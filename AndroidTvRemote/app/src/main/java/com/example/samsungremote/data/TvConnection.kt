package com.example.samsungremote.data

data class TvConnection(
    val id: String = "",
    val name: String = "",
    val ip: String = "",
    val port: Int = 8002,
    val token: String? = null
)