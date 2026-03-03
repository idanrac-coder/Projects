package com.novachat.core.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

object AuroraColors {
    val ElectricViolet = Color(0xFF6C5CE7)
    val SoftLavender = Color(0xFFA29BFE)
    val TealSpark = Color(0xFF00CEC9)
    val MintGlow = Color(0xFF55EFC4)
    val RoseBlush = Color(0xFFFD79A8)

    val GhostWhite = Color(0xFFF8F9FE)
    val PureWhite = Color(0xFFFFFFFF)
    val LavenderMist = Color(0xFFF0EFFF)
    val LightDivider = Color(0xFFE8E8F0)

    val DeepSpace = Color(0xFF1A1B2E)
    val Obsidian = Color(0xFF0F1023)
    val NightSurface = Color(0xFF232446)
    val NightElevated = Color(0xFF2A2B4A)
    val DarkDivider = Color(0xFF2A2B4A)

    val Error = Color(0xFFFF6B6B)
    val Success = Color(0xFF00B894)
    val SuccessLight = Color(0xFF55EFC4)
    val Warning = Color(0xFFFDCB6E)

    val OnLightPrimary = Color(0xFF1A1B2E)
    val OnLightSecondary = Color(0xFF6B6B8A)
    val OnDarkPrimary = Color(0xFFE8E8F0)
    val OnDarkSecondary = Color(0xFF9393B0)
}

private val avatarGradients = listOf(
    listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE)),
    listOf(Color(0xFF00CEC9), Color(0xFF55EFC4)),
    listOf(Color(0xFFFD79A8), Color(0xFFFDCB6E)),
    listOf(Color(0xFFE17055), Color(0xFFFAB1A0)),
    listOf(Color(0xFF0984E3), Color(0xFF74B9FF)),
    listOf(Color(0xFF00B894), Color(0xFF55EFC4)),
    listOf(Color(0xFFE84393), Color(0xFFFD79A8)),
    listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9)),
    listOf(Color(0xFFFF7675), Color(0xFFFD79A8)),
    listOf(Color(0xFF636E72), Color(0xFFB2BEC3)),
    listOf(Color(0xFFD63031), Color(0xFFFF7675)),
    listOf(Color(0xFF0984E3), Color(0xFF6C5CE7)),
    listOf(Color(0xFFFDCB6E), Color(0xFFE17055)),
    listOf(Color(0xFF00B894), Color(0xFF0984E3)),
    listOf(Color(0xFFE84393), Color(0xFF6C5CE7)),
    listOf(Color(0xFF00CEC9), Color(0xFF0984E3)),
)

fun avatarGradient(address: String): Brush {
    val index = abs(address.hashCode()) % avatarGradients.size
    return Brush.linearGradient(
        colors = avatarGradients[index],
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}

fun avatarGradientColors(address: String): List<Color> {
    val index = abs(address.hashCode()) % avatarGradients.size
    return avatarGradients[index]
}

fun Color.brighten(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

fun sentBubbleGradient(baseColor: Color): Brush {
    return Brush.verticalGradient(
        colors = listOf(baseColor.brighten(0.08f), baseColor)
    )
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 200f, 0f),
            end = Offset(translateAnim, 0f)
        )
    )
}

@Composable
fun ShimmerConversationItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
fun GradientAvatar(
    address: String,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Int = 52
) {
    val sanitized = displayName.replace(Regex("[\\uFFFC-\\uFFFF\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F]"), "").trim()
    val initials = sanitized
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(avatarGradient(address)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (initials.isNotEmpty()) initials else "#",
            color = Color.White,
            fontSize = (size * 0.34f).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
