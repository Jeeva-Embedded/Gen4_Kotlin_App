package com.gen4.spinning.core.bt

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Scanning : ConnectionState()
    data class Connecting(val deviceName: String, val deviceAddress: String) : ConnectionState()
    data class Connected(val deviceName: String, val deviceAddress: String) : ConnectionState()
    data class Reconnecting(val deviceName: String, val deviceAddress: String) : ConnectionState()
    data class Lost(val deviceName: String, val deviceAddress: String) : ConnectionState()
}

const val RECONNECT_WARN_TIMEOUT_MS = 3_000L
const val RECONNECT_LOST_TIMEOUT_MS = 15_000L
const val CONNECT_TIMEOUT_MS = 12_000L
const val FIRST_FRAME_TIMEOUT_MS = 5_000L
