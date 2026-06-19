package com.samsungremote

/**
 * Represents the lifecycle state of the connection to the Samsung TV.
 */
sealed interface TvConnectionState {
    data object Idle : TvConnectionState

    data object Discovering : TvConnectionState

    data object Connecting : TvConnectionState

    data class Connected(
        val ip: String,
        val mac: String,
        val deviceName: String = ""
    ) : TvConnectionState

    data class Disconnected(
        val reason: String = ""
    ) : TvConnectionState

    data class Error(
        val throwable: Throwable
    ) : TvConnectionState
}
