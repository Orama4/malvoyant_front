package com.example.malvoayant.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malvoayant.ui.theme.AppColors

/**
 * Reusable navigation button component
 */
@Composable
fun NavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, fontSize = 18.sp)
    }
}