package com.example.malvoayant.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.delay
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // GIF support
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
        }
        .build()

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/logo.gif")
            .build(),
        imageLoader = imageLoader
    )

    // State for opacity
    var targetAlpha by remember { mutableStateOf(0f) }

    // Animate alpha toward targetAlpha
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 1000)
    )

    // Global launch effect for the animation sequence
    LaunchedEffect(Unit) {
        targetAlpha = 1f           // Fade-in
        delay(2000)                // Wait while fully visible
        targetAlpha = 0f           // Fade-out
        delay(1000)                // Wait for fade-out to complete
        onSplashFinished()         // Trigger navigation
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = "Splash Animation",
            modifier = Modifier
                .size(350.dp)
                .fillMaxWidth()
                .alpha(animatedAlpha), // animated alpha
            contentScale = ContentScale.Fit
        )
    }
}
