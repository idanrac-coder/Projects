package com.novachat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws bold, irregular comic panel borders over the chat area.
 * Segments the screen like a comic strip.
 */
@Composable
fun ComicPanelOverlay(
    modifier: Modifier = Modifier,
    strokeWidth: Float = 3f,
    color: Color = Color.Black,
    alpha: Float = 0.5f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = color.copy(alpha = alpha)
        val path = Path().apply {
            moveTo(0f, h * 0.45f)
            lineTo(w * 0.2f, h * 0.43f)
            lineTo(w * 0.5f, h * 0.47f)
            lineTo(w * 0.8f, h * 0.44f)
            lineTo(w, h * 0.46f)
        }
        drawPath(path, color = c, style = Stroke(width = strokeWidth))
        val path2 = Path().apply {
            moveTo(w * 0.97f, 0f)
            lineTo(w * 0.99f, h * 0.3f)
            lineTo(w * 0.96f, h * 0.6f)
            lineTo(w * 0.98f, h)
        }
        drawPath(path2, color = c, style = Stroke(width = strokeWidth))
    }
}
