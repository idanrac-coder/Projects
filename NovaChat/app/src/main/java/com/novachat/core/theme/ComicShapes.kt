package com.novachat.core.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Jagged / explosion-style shape for comic mode sent bubbles.
 * Creates irregular edges resembling hand-drawn comic speech bubbles.
 */
val ComicSentBubbleShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val j = 8f
    moveTo(0f, h * 0.4f)
    lineTo(0f, h - j)
    lineTo(j, h)
    lineTo(w * 0.2f - j, h)
    lineTo(w * 0.2f, h - j)
    lineTo(w * 0.35f, h)
    lineTo(w * 0.5f - j, h)
    lineTo(w * 0.5f, h - j)
    lineTo(w * 0.65f + j, h)
    lineTo(w * 0.8f, h)
    lineTo(w * 0.8f + j, h - j)
    lineTo(w - j, h)
    lineTo(w, h - j)
    lineTo(w, j)
    lineTo(w - j, 0f)
    lineTo(w * 0.8f + j, 0f)
    lineTo(w * 0.8f, j)
    lineTo(w * 0.5f + j, 0f)
    lineTo(w * 0.5f, j)
    lineTo(w * 0.2f - j, 0f)
    lineTo(w * 0.2f, j)
    lineTo(j, 0f)
    lineTo(0f, j)
    close()
}

/**
 * Cloud-style shape for comic mode received bubbles.
 * Softer, rounded outline like classic comic speech clouds.
 */
val ComicReceivedBubbleShape: Shape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 24.dp,
    bottomEnd = 4.dp
)
