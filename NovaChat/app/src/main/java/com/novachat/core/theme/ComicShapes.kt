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
 * Irregular cloud-style shape for comic mode received bubbles.
 * Jagged outline like classic comic speech clouds.
 */
val ComicReceivedBubbleShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val bump = 6f
    moveTo(12f, h * 0.5f)
    lineTo(12f, h - 12f)
    lineTo(w * 0.2f, h - 6f)
    lineTo(w * 0.35f, h)
    lineTo(w * 0.5f, h - 8f)
    lineTo(w * 0.65f, h)
    lineTo(w * 0.8f, h - 6f)
    lineTo(w - 12f, h - 12f)
    lineTo(w - 12f, h * 0.6f)
    lineTo(w - 6f, 12f)
    lineTo(w * 0.8f, 6f)
    lineTo(w * 0.6f, 12f)
    lineTo(w * 0.5f, 6f)
    lineTo(w * 0.35f, 12f)
    lineTo(w * 0.2f, 6f)
    lineTo(12f, 12f)
    close()
}

/**
 * Irregular speech bubble shape for comic mode input field.
 */
val ComicInputBubbleShape: Shape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    bottomEnd = 8.dp
)
