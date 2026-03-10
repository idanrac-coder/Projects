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
 * Halftone dot pattern overlay for comic mode background.
 * Draws a grid of dots resembling classic comic book printing.
 */
@Composable
fun HalftoneBackground(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.Black,
    dotAlpha: Float = 0.08f,
    spacing: Float = 12f
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
    val radius = (spacing * 0.35f).coerceAtLeast(1f)
    var y = spacing
    while (y < size.height + spacing) {
        val offsetX = if ((y / spacing).toInt() % 2 == 0) 0f else spacing / 2f
        var x = offsetX
        while (x < size.width + spacing) {
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(x, y),
                alpha = alpha
            )
            x += spacing
        }
        y += spacing
    }
}
