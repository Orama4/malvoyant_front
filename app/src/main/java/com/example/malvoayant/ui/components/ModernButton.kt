package com.example.malvoayant.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.malvoayant.ui.theme.AppColors
import kotlinx.coroutines.delay

// --- Reusable Modern Button ---
@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isDestructive: Boolean = false // Flag for error color scheme
) {
    val containerColor = when {
        !enabled -> Color(0xFF1D1D1F).copy(alpha = 0.12f) // Standard disabled color
        isDestructive -> Color(0xFFFF3B30)
        else -> AppColors.primary // Orange default
    }
    val contentColor = when {
        !enabled -> Color(0xFF1D1D1F).copy(alpha = 0.38f)
        isDestructive -> Color.White
        else -> Color.White // White on orange
    }

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp).padding(horizontal = 15.dp), // Slightly taller button
        enabled = enabled,
        shape = MaterialTheme.shapes.small, // Consistent rounding
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFF1D1D1F).copy(alpha = 0.08f), // Adjusted disabled bg
            disabledContentColor = Color(0xFF1D1D1F).copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation( // No shadow for flat look
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )

    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}


/**
 * Un indicateur de progression circulaire personnalisé avec des couleurs orange et blanc
 * qui s'affiche au centre de l'écran avec une animation fluide
 */
@Composable
fun ElegantCircularProgressIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFFFF7D3C),  // Orange
            trackColor = Color.White.copy(alpha = 0.3f),
            strokeWidth = 4.dp
        )
    }
}


/**
 * Affiche un message d'erreur dans un popup élégant
 * @param errorMessage Le message d'erreur à afficher
 * @param onDismiss Callback appelé lorsque le popup est fermé
 */

@Composable
fun AmazingErrorPopup(
    errorMessage: String
) {
    val errorColor = Color(0xFFFF5722)
    val  textColor= Color(0xFF333333)
    val backgroundColor = Color.White

    // État local pour gérer la visibilité du dialogue
    val isVisible = remember { mutableStateOf(true) }

    // Animation d'entrée
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    if (isVisible.value) {
        Dialog(
            onDismissRequest = { isVisible.value = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .widthIn(max = 320.dp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Icône de fermeture (X)
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { isVisible.value = false }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Icône d'erreur animée
                    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
                    val iconScale = pulseAnimation.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "iconPulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .scale(iconScale.value)
                            .clip(CircleShape)
                            .background(errorColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "!",
                            color = textColor,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Titre
                    Text(
                        text = "Erreur",
                        color = errorColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message d'erreur
                    Text(
                        text = errorMessage,
                        color = textColor,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bouton de fermeture
                    Button(
                        onClick = { isVisible.value = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = errorColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Fermer",
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}