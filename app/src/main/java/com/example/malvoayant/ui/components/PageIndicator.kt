package com.example.malvoayant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview



@Composable
fun PageIndicator(
    totalDots: Int, // Total number of dots
    selectedIndex: Int, // Active dot index
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalDots) {
            Box(
                modifier = Modifier
                    .width(if (i == selectedIndex) 12.dp else 10.dp)
                    .height(if (i == selectedIndex) 12.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (i == selectedIndex) Color(0xFFFF9800) // Active color (orange)
                        else Color(0xFFFFCC80).copy(alpha = 0.6f) // Inactive color (lighter & faded)
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PageIndicatorPreview() {
    PageIndicator(totalDots = 5, selectedIndex = 2)
}

