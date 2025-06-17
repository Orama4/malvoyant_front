package com.example.malvoayant.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.malvoayant.ui.utils.BoundingBox

class DetectionOverlayDefaults {
    companion object {
        val BoxColor = Color.Red
        val TextColor = Color.White
        val TextBackgroundColor = Color.Black
        val TextSizeDp = 16.dp
        val StrokeWidthDp = 2.dp
    }
}

@Composable
fun DetectionOverlay(
    boundingBoxes: List<BoundingBox>,
    boxColor: Color = DetectionOverlayDefaults.BoxColor,
    textColor: Color = DetectionOverlayDefaults.TextColor,
    textBackgroundColor: Color = DetectionOverlayDefaults.TextBackgroundColor,
    textSizeDp: Dp = DetectionOverlayDefaults.TextSizeDp,
    strokeWidthDp: Dp = DetectionOverlayDefaults.StrokeWidthDp,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {

        val canvasWidth = size.width
        val canvasHeight = size.height

        val strokeWidthPx = strokeWidthDp.toPx()
        val textSizePx = textSizeDp.toPx()

        val boxPaint = Paint().apply {
            this.color = boxColor
            this.strokeWidth = strokeWidthPx
            this.style = androidx.compose.ui.graphics.PaintingStyle.Stroke
        }

        val textBackgroundPaint = Paint().apply {
            this.color = textBackgroundColor
            this.style = androidx.compose.ui.graphics.PaintingStyle.Fill
        }

        val textPaint = android.graphics.Paint().apply {
            color = textColor.toArgb()
            textSize = textSizePx
            isAntiAlias = true
        }

        boundingBoxes.forEach { box ->

            val left = box.x1 * canvasWidth
            val top = box.y1 * canvasHeight
            val right = box.x2 * canvasWidth
            val bottom = box.y2 * canvasHeight

            // Draw the rectangle around detected object
            drawRect(
                color = boxColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
            )

            // Draw label
            drawIntoCanvas { canvas ->

                val label = box.clsName

                // Measure text
                val textWidth = textPaint.measureText(label)
                val fontMetrics = textPaint.fontMetrics
                val textHeight = fontMetrics.descent - fontMetrics.ascent

// Draw background rectangle behind text
                canvas.drawRect(
                    Rect(
                        left,
                        top,
                        left + textWidth + 8f,
                        top + textHeight + 8f
                    ),
                    textBackgroundPaint
                )

// Draw the text itself
                canvas.nativeCanvas.drawText(
                    label,
                    left,
                    top - fontMetrics.ascent + 4f, // Position text properly inside the background
                    textPaint
                )

            }
        }
    }
}
