package com.example.malvoayant.ui.utils



data class ImageAnalysisResult(
    val visualMatch: Boolean,
    val text: String,
    val keywordDetected: Boolean
)

data class BatchAnalysisResult(
    val visualMatches: Int,
    val keywordMatches: Int,
    val texts: List<String>,
    val totalImages: Int
)
