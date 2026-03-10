package com.novachat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private data class ActionWord(
    val text: String,
    val color: Color,
    val fontSize: Int,
    val xPercent: Float,
    val yPercent: Float,
    val rotation: Float
)

private val ACTION_WORDS = listOf(
    ActionWord("BAM!", Color(0xFFDC2626), 56, 0.1f, 0.2f, -12f),
    ActionWord("BAM!", Color(0xFFDC2626), 48, 0.2f, 0.6f, 8f),
    ActionWord("CRASH!", Color(0xFF92400E), 52, 0.72f, 0.35f, -8f),
    ActionWord("POW!", Color(0xFFDC2626), 44, 0.82f, 0.72f, 15f)
)

/**
 * Bold comic action words overlay with radial bursts.
 * Red/brown colors, black outline, integrated into panel background.
 */
@Composable
fun ComicActionOverlay(
    modifier: Modifier = Modifier,
    alpha: Float = 0.45f
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val boxSizePx = with(density) { 120.dp.toPx() }
        ACTION_WORDS.forEach { word ->
            val x = w * word.xPercent - boxSizePx / 2
            val y = h * word.yPercent - boxSizePx / 4
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(x.toInt(), y.toInt()) }
                    .graphicsLayer {
                        rotationZ = word.rotation
                        this.alpha = alpha
                    }
                    .size(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRadialBurst(
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f
                    )
                }
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedComicText(
                        text = word.text,
                        color = word.color,
                        fontSize = word.fontSize
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawRadialBurst(center: Offset, radius: Float) {
    val burstColor = Color(0xFFFCD34D).copy(alpha = 0.25f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(burstColor, Color.Transparent),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
    for (i in 0 until 12) {
        val angle = (i * 30) * Math.PI / 180
        val start = Offset(
            center.x + (radius * 0.3f * cos(angle).toFloat()),
            center.y + (radius * 0.3f * sin(angle).toFloat())
        )
        val end = Offset(
            center.x + (radius * cos(angle).toFloat()),
            center.y + (radius * sin(angle).toFloat())
        )
        drawLine(
            color = Color(0xFF92400E).copy(alpha = 0.2f),
            start = start,
            end = end,
            strokeWidth = 2f
        )
    }
}

@Composable
private fun OutlinedComicText(
    text: String,
    color: Color,
    fontSize: Int
) {
    val stroke = 2.dp
    Box(contentAlignment = Alignment.Center) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx != 0 || dy != 0) {
                    Text(
                        text = text,
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.offset(
                            x = (dx * stroke.value).dp,
                            y = (dy * stroke.value).dp
                        )
                    )
                }
            }
        }
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}
