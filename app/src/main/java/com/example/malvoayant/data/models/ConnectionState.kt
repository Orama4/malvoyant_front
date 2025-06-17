package com.example.malvoayant.data.models

/*data class ConnectionState(
    val isConnected: Boolean = false,
    val error: String? = null,
    val lastLocationSent: Pair<Float, Float>? = null
)

 */

data class ConnectionState(
    val isConnected: Boolean = false,
    val error: String? = null,
    val lastLocationSent: Pair<Float, Float>? = null,
    val lastLocationTime: Long = 0L,
    val lastConnectionTime: Long = 0L,
    val lastHeartbeatTime: Long = 0L,
    val lastConfirmationTime: Long = 0L
)