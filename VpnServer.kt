package com.example.vpn

data class VpnServer(
    val hostName: String,
    val ip: String,
    val countryLong: String,
    val countryShort: String,
    val ping: Long,
    val speed: Long,
    val ovpnConfigBase64: String,
    val score: Long = 0L,
    val sstpSupported: Boolean = false,
    val sstpPort: Int = 443,
    val isTcp443: Boolean = false
)
