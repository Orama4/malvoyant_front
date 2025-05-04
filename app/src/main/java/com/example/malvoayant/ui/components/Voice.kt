package com.example.voicerecorder

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.utils.startListening

@Composable
fun VoiceRecorderButton(
    onVoiceInput: (String) -> Unit // Callback to handle recognized speech text
) {
    var isRecording by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val animatedValues = List(3) { index ->
        val infiniteTransition = rememberInfiniteTransition()
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = index * 400,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Box(
        modifier = Modifier
            .size(160.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isRecording = !isRecording
                        if (isRecording) {
                            // Start voice recognition
                            startListening(context) { recognizedText ->
                                onVoiceInput(recognizedText) // Pass recognized text to the callback
                                isRecording = false // Stop recording after recognition
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            if (isRecording) {
                animatedValues.forEachIndexed { index, animation ->
                    drawRippleLayer(
                        color = AppColors.primary,
                        alpha = (1f - (animation.value - 1f)) * 0.3f,
                        scale = animation.value
                    )
                }
            }

            drawCircle(
                color = AppColors.primary,
                radius = size.minDimension / 4
            )
        }

        Icon(
            modifier = Modifier.size(50.dp),
            painter = painterResource(id = android.R.drawable.ic_btn_speak_now),
            contentDescription = "Voice Guide",
            tint = Color.White
        )
    }
}

private fun DrawScope.drawRippleLayer(
    color: Color,
    alpha: Float,
    scale: Float
) {
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = (size.minDimension / 4) * scale,
        alpha = alpha
    )
}
