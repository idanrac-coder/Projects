package com.novachat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * Halftone (Ben-Day) dot pattern overlay for comic mode background.
 * Denser dots, warm brown/beige tint, classic comic printing feel.
 */
@Composable
fun HalftoneBackground(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF6B5344),
    dotAlpha: Float = 0.12f,
    spacing: Float = 8f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawHalftoneDots(
            color = dotColor,
            alpha = dotAlpha,
            spacing = spacing
        )
    }
}

private fun DrawScope.drawHalftoneDots(
    color: Color,
    alpha: Float,
    spacing: Float
) {
    val baseRadius = (spacing * 0.4f).coerceAtLeast(1f)
    var yi = 0
    var y = spacing
    while (y < size.height + spacing) {
        val offsetX = if (yi % 2 == 0) 0f else spacing / 2f
        var x = offsetX
        var xi = 0
        while (x < size.width + spacing) {
            val radius = baseRadius * (0.8f + (xi + yi) % 3 * 0.1f)
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(x, y),
                alpha = alpha
            )
            x += spacing
            xi++
        }
        y += spacing
        yi++
    }
}
