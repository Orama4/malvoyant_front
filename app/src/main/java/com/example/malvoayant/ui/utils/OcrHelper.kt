package com.example.malvoayant.ui.utils


import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object OcrHelper {

    suspend fun performOcr(context: Context, bitmap: Bitmap, languages: List<String> = listOf("en", "ar")): String {
        val image = InputImage.fromBitmap(bitmap, 0)

        val recognizers = mutableListOf<com.google.mlkit.vision.text.TextRecognizer>()

        if ("en" in languages) {
            recognizers.add(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS))
        }


        val allText = StringBuilder()
        for (recognizer in recognizers) {
            val result = recognizer.process(image).await()
            allText.append(result.text).append("\n")
        }

        return allText.toString().trim()
    }
}
