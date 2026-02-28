package com.novachat.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme

data class ChatColors(
    val sentBubble: Color,
    val receivedBubble: Color,
    val sentText: Color,
    val receivedText: Color
)

data class ChatShapes(
    val sentBubbleShape: Shape,
    val receivedBubbleShape: Shape
)

val LocalChatColors = compositionLocalOf {
    ChatColors(
        sentBubble = Color(0xFF6750A4),
        receivedBubble = Color(0xFFE8DEF8),
        sentText = Color.White,
        receivedText = Color(0xFF1D1B20)
    )
}

val LocalChatShapes = compositionLocalOf {
    ChatShapes(
        sentBubbleShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
        receivedBubbleShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    )
}

val LocalActiveTheme = compositionLocalOf { BuiltInThemes.all.first() }

@Composable
fun NovaChatMaterialTheme(
    activeTheme: NovaChatTheme? = null,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val colorScheme = when {
        useDynamicColor && activeTheme == null -> {
            if (isDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        activeTheme != null -> {
            lightColorScheme(
                primary = Color(activeTheme.primaryColor),
                secondary = Color(activeTheme.secondaryColor),
                surface = Color(activeTheme.surfaceColor),
                background = Color(activeTheme.backgroundColor),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onSurface = Color(0xFF1D1B20),
                onBackground = Color(0xFF1D1B20)
            )
        }
        else -> {
            if (isDark) darkColorScheme() else lightColorScheme()
        }
    }

    val chatColors = if (activeTheme != null) {
        ChatColors(
            sentBubble = Color(activeTheme.sentBubbleColor),
            receivedBubble = Color(activeTheme.receivedBubbleColor),
            sentText = Color(activeTheme.sentTextColor),
            receivedText = Color(activeTheme.receivedTextColor)
        )
    } else {
        LocalChatColors.current
    }

    val chatShapes = if (activeTheme != null) {
        bubbleShapeFor(activeTheme.bubbleShape)
    } else {
        LocalChatShapes.current
    }

    val theme = activeTheme ?: BuiltInThemes.all.first()

    CompositionLocalProvider(
        LocalChatColors provides chatColors,
        LocalChatShapes provides chatShapes,
        LocalActiveTheme provides theme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

fun bubbleShapeFor(shape: BubbleShape): ChatShapes = when (shape) {
    BubbleShape.ROUNDED -> ChatShapes(
        sentBubbleShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
        receivedBubbleShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    )
    BubbleShape.SQUARE -> ChatShapes(
        sentBubbleShape = RoundedCornerShape(4.dp),
        receivedBubbleShape = RoundedCornerShape(4.dp)
    )
    BubbleShape.CLOUD -> ChatShapes(
        sentBubbleShape = RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp),
        receivedBubbleShape = RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp)
    )
    BubbleShape.MINIMAL -> ChatShapes(
        sentBubbleShape = RoundedCornerShape(12.dp),
        receivedBubbleShape = RoundedCornerShape(12.dp)
    )
}
