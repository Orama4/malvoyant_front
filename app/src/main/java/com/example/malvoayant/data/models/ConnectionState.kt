package com.example.malvoayant.data.models

data class ConnectionState(
    val isConnected: Boolean = false,
    val error: String? = null,
    val lastLocationSent: Pair<Float, Float>? = null
)