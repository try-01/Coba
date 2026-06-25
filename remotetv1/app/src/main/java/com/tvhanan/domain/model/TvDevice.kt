package com.tvhanan.domain.model

data class TvDevice(
    val ipAddress: String,
    val name: String = "Samsung TV",
    val macAddress: String? = null,
    val port: Int = 8001,
    val token: String? = null
)
