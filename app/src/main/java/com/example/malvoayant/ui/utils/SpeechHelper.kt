package com.example.malvoayant.ui.utils

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class SpeechHelper(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val TAG = "com.example.malvoayant.ui.utils.SpeechHelper"
    private val pendingQueue = mutableListOf<String>()
    // In your SpeechHelper.kt
    fun initializeSpeech(text: String) {
        Log.e(TAG, "BEGIN INITIALIZATION")

        try {
            tts = TextToSpeech(context) { status ->
                Log.e(TAG, "TTS init callback received with status: $status")

                if (status == TextToSpeech.SUCCESS) {
                    // First try with default locale
                    var langResult = tts?.setLanguage(Locale.getDefault())
                    Log.e(TAG, "Default language result: $langResult")

                    // If default fails, try US English
                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        langResult = tts?.setLanguage(Locale.US)
                        Log.e(TAG, "US English result: $langResult")
                    }

                    // If US English fails, try with available languages
                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Get available languages and use the first one
                        val availableLangs = tts?.availableLanguages
                        Log.e(TAG, "Available languages: $availableLangs")

                        if (availableLangs?.isNotEmpty() == true) {
                            langResult = tts?.setLanguage(availableLangs.first())
                            Log.e(TAG, "Using first available language: ${availableLangs.first()}, result: $langResult")
                        }
                    }

                    // Check if we successfully set a language
                    if (langResult != TextToSpeech.LANG_MISSING_DATA &&
                        langResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TTS SUCCESSFULLY INITIALIZED with working language!")
                        isReady = true

                        // Use a lower pitch and speech rate for better clarity
                        tts?.setPitch(0.8f)
                        tts?.setSpeechRate(0.8f)

                        // Speak any pending messages
                        if (pendingQueue.isNotEmpty()) {
                            Log.e(TAG, "Speaking ${pendingQueue.size} pending messages")
                            pendingQueue.forEach { speakText(it) }
                            pendingQueue.clear()
                        }
                        //speeck the text
                        speak(text)
                    } else {
                        Log.e(TAG, "No suitable language found for TTS")
                        isReady = false
                    }
                } else {
                    Log.e(TAG, "TTS initialization FAILED with status: $status")
                    isReady = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during initialization: ${e.message}")
            e.printStackTrace()
        }
    }
    // Check if TTS is available on the device
    private fun isTtsAvailable(context: Context): Boolean {
        val pm = context.packageManager
        val activities = pm.queryIntentActivities(
            Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA), 0)

        Log.e(TAG, "Found ${activities.size} TTS activities")
        return activities.isNotEmpty()
    }

    fun speak(text: String) {
        if (isReady && tts != null) {
            speakText(text)
        } else {
            Log.e(TAG, "Adding to pending queue: $text")
            pendingQueue.add(text)

            // Try to initialize if not already initialized
            if (tts == null) {
                initializeSpeech(text)
            }
        }
    }

    private fun speakText(text: String) {
        try {
            Log.e(TAG, "Speaking: $text")
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception while speaking: ${e.message}")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}