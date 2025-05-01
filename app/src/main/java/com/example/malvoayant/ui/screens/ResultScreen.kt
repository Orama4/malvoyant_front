package com.example.malvoayant.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.malvoayant.ui.utils.ImageAnalyzer

@Composable
fun ResultScreen(
    navController: NavController,
    bitmaps: List<Bitmap>
) {
    val context = LocalContext.current
    var resultText by remember { mutableStateOf("Processing...") }
/*
    LaunchedEffect(Unit) {
        val result = ImageAnalyzer.analyzeImages(
            context, bitmaps,
            poiImages = TODO(),
            keywords = TODO()
        )
        resultText = result.toString()
    }*/


    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Results", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = resultText)
    }
}
