package com.example.malvoayant.data.websocket

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.channels.Channel
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

class CameraWebSocketClient(
    serverUri: URI,
    private val frameChannel: Channel<Bitmap>
) : WebSocketClient(serverUri) {

    override fun onOpen(handshakedata: ServerHandshake?) {
        println("WebSocket Opened")
    }

    override fun onMessage(message: String?) {
        println("Received text message (ignored)")
    }
    override fun onMessage(bytes: ByteBuffer?) {
        bytes?.let {
            val byteArray = ByteArray(bytes.remaining())
            bytes.get(byteArray)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            if (bitmap != null) {
                frameChannel.trySend(bitmap)
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println("WebSocket Closed: $reason")
    }

    override fun onError(ex: Exception?) {
        ex?.printStackTrace()
    }
}
