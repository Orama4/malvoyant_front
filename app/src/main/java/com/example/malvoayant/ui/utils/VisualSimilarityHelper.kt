package com.example.malvoayant.ui.utils
import android.graphics.Bitmap
import androidx.core.graphics.get
import kotlin.math.abs

object VisualSimilarityHelper {

    fun averageHash(bitmap: Bitmap): Long {
        val resized = Bitmap.createScaledBitmap(bitmap, 8, 8, false)
        val pixels = IntArray(64)
        resized.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        val gray = pixels.map { it and 0xFF } // get grayscale
        val avg = gray.average()
        return gray.foldIndexed(0L) { i, acc, px -> acc or ((if (px > avg) 1L else 0L) shl i) }
    }

    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    fun isVisuallySimilar(b1: Bitmap, b2: Bitmap, threshold: Int = 10): Boolean {
        val h1 = averageHash(b1)
        val h2 = averageHash(b2)
        return hammingDistance(h1, h2) <= threshold
    }
}
