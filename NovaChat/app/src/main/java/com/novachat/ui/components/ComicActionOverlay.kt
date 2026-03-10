package com.novachat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ACTION_WORDS = listOf("POW!", "BAM!", "ZAP!", "KA-POW!")

/**
 * Faded comic action words overlay for comic mode.
 * Positions words at corners/edges for a dynamic comic feel.
 */
@Composable
fun ComicActionOverlay(
    modifier: Modifier = Modifier,
    alpha: Float = 0.12f
) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Top-left POW!
        Text(
            text = ACTION_WORDS[0],
            modifier = Modifier
                .align(Alignment.TopStart)
                .graphicsLayer {
                    translationX = 40f
                    translationY = 80f
                    rotationZ = -15f
                }
                .alpha(alpha),
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFF59E0B)
        )
        // Bottom-right BAM!
        Text(
            text = ACTION_WORDS[1],
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    translationX = -60f
                    translationY = -120f
                    rotationZ = 12f
                }
                .alpha(alpha),
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFEA580C)
        )
        // Center-right ZAP! (subtle)
        Text(
            text = ACTION_WORDS[2],
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    translationX = -80f
                    rotationZ = -8f
                }
                .alpha(alpha * 0.7f),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFBBF24)
        )
    }
}
