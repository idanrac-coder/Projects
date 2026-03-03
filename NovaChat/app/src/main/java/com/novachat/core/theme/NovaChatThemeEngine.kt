package com.novachat.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.model.WallpaperType

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

data class ChatWallpaper(
    val type: WallpaperType = WallpaperType.SOLID,
    val value: String = "",
    val primaryColor: Color = Color.Transparent,
    val secondaryColor: Color = Color.Transparent
)

val LocalChatColors = compositionLocalOf {
    ChatColors(
        sentBubble = Color(0xFF6C5CE7),
        receivedBubble = Color(0xFFF0EFFF),
        sentText = Color(0xFFFFFFFF),
        receivedText = Color(0xFF1A1B2E)
    )
}

val LocalChatShapes = compositionLocalOf {
    ChatShapes(
        sentBubbleShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
        receivedBubbleShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    )
}

val LocalChatWallpaper = compositionLocalOf { ChatWallpaper() }

val LocalActiveTheme = compositionLocalOf { BuiltInThemes.all.first() }

private val AuroraTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun NovaChatMaterialTheme(
    activeTheme: NovaChatTheme? = null,
    useDynamicColor: Boolean = false,
    bubbleShapeOverride: BubbleShape? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val theme = activeTheme ?: BuiltInThemes.all.first()

    val colorScheme = when {
        useDynamicColor && activeTheme == null -> {
            if (isDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        else -> {
            val primary = Color(theme.primaryColor)
            val secondary = Color(theme.secondaryColor)
            val surface = Color(theme.surfaceColor)
            val background = Color(theme.backgroundColor)
            val surfaceLuminance = surface.luminance()
            val isDarkSurface = surfaceLuminance < 0.4f

            val onSurface = if (isDarkSurface) Color(0xFFE8E8F0) else Color(0xFF1A1B2E)
            val onSurfaceVariant = if (isDarkSurface) Color(0xFF9393B0) else Color(0xFF6B6B8A)
            val onBackground = if (background.luminance() < 0.4f) Color(0xFFE8E8F0) else Color(0xFF1A1B2E)
            val onBackgroundVariant = if (background.luminance() < 0.4f) Color(0xFF9393B0) else Color(0xFF6B6B8A)

            val surfaceVariant = if (isDarkSurface)
                Color(
                    primary.red * 0.08f + surface.red * 0.92f,
                    primary.green * 0.08f + surface.green * 0.92f,
                    primary.blue * 0.08f + surface.blue * 0.92f
                )
            else
                Color(
                    primary.red * 0.06f + surface.red * 0.94f,
                    primary.green * 0.06f + surface.green * 0.94f,
                    primary.blue * 0.06f + surface.blue * 0.94f
                )

            val outline = if (isDarkSurface) Color(0xFF4A4A6A) else Color(0xFFC0C0D0)
            val outlineVariant = if (isDarkSurface) Color(0xFF3A3A5A) else Color(0xFFE0E0EC)

            val primaryContainer = if (isDarkSurface)
                Color(primary.red * 0.2f, primary.green * 0.2f, primary.blue * 0.2f)
            else
                Color(
                    primary.red * 0.12f + 0.88f,
                    primary.green * 0.12f + 0.88f,
                    primary.blue * 0.12f + 0.88f
                )

            val secondaryContainer = if (isDarkSurface)
                Color(secondary.red * 0.15f, secondary.green * 0.15f, secondary.blue * 0.15f)
            else
                Color(
                    secondary.red * 0.1f + 0.9f,
                    secondary.green * 0.1f + 0.9f,
                    secondary.blue * 0.1f + 0.9f
                )

            val tertiary = if (isDarkSurface) AuroraColors.RoseBlush else Color(0xFFE84393)

            val errorColor = AuroraColors.Error
            val errorContainer = if (isDarkSurface) Color(0xFF3D1515) else Color(0xFFFFE5E5)

            val surfaceBright = if (isDarkSurface) surface.brighten(0.08f) else surface
            val surfaceDim = if (isDarkSurface) surface else surface.copy(
                red = surface.red * 0.96f,
                green = surface.green * 0.96f,
                blue = surface.blue * 0.96f
            )

            val surfaceContainerLowest = if (isDarkSurface)
                background
            else
                background
            val surfaceContainerLow = if (isDarkSurface)
                Color(surface.red * 0.95f + 0.02f, surface.green * 0.95f + 0.02f, surface.blue * 0.95f + 0.02f)
            else
                Color(
                    (surface.red + background.red) / 2f,
                    (surface.green + background.green) / 2f,
                    (surface.blue + background.blue) / 2f
                )
            val surfaceContainer = surface
            val surfaceContainerHigh = if (isDarkSurface)
                surface.brighten(0.05f)
            else
                surface
            val surfaceContainerHighest = if (isDarkSurface)
                surface.brighten(0.1f)
            else
                surface

            if (isDarkSurface) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = primary.brighten(0.4f),
                    secondary = secondary,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryContainer,
                    onSecondaryContainer = secondary.brighten(0.4f),
                    tertiary = tertiary,
                    onTertiary = Color.White,
                    surface = surface,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant,
                    surfaceVariant = surfaceVariant,
                    background = background,
                    onBackground = onBackground,
                    outline = outline,
                    outlineVariant = outlineVariant,
                    error = errorColor,
                    onError = Color.White,
                    errorContainer = errorContainer,
                    onErrorContainer = Color(0xFFFFB4AB),
                    surfaceBright = surfaceBright,
                    surfaceDim = surfaceDim,
                    surfaceContainerLowest = surfaceContainerLowest,
                    surfaceContainerLow = surfaceContainerLow,
                    surfaceContainer = surfaceContainer,
                    surfaceContainerHigh = surfaceContainerHigh,
                    surfaceContainerHighest = surfaceContainerHighest,
                    inverseSurface = Color(0xFFE8E8F0),
                    inverseOnSurface = Color(0xFF1A1B2E),
                    inversePrimary = primary
                )
            } else {
                lightColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = Color(
                        primary.red * 0.4f,
                        primary.green * 0.4f,
                        primary.blue * 0.4f
                    ),
                    secondary = secondary,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryContainer,
                    onSecondaryContainer = Color(
                        secondary.red * 0.4f,
                        secondary.green * 0.4f,
                        secondary.blue * 0.4f
                    ),
                    tertiary = tertiary,
                    onTertiary = Color.White,
                    surface = surface,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant,
                    surfaceVariant = surfaceVariant,
                    background = background,
                    onBackground = onBackground,
                    outline = outline,
                    outlineVariant = outlineVariant,
                    error = errorColor,
                    onError = Color.White,
                    errorContainer = errorContainer,
                    onErrorContainer = Color(0xFF8B1A1A),
                    surfaceBright = surfaceBright,
                    surfaceDim = surfaceDim,
                    surfaceContainerLowest = surfaceContainerLowest,
                    surfaceContainerLow = surfaceContainerLow,
                    surfaceContainer = surfaceContainer,
                    surfaceContainerHigh = surfaceContainerHigh,
                    surfaceContainerHighest = surfaceContainerHighest,
                    inverseSurface = Color(0xFF1A1B2E),
                    inverseOnSurface = Color(0xFFE8E8F0),
                    inversePrimary = primary.brighten(0.3f)
                )
            }
        }
    }

    val chatColors = ChatColors(
        sentBubble = Color(theme.sentBubbleColor),
        receivedBubble = Color(theme.receivedBubbleColor),
        sentText = Color(theme.sentTextColor),
        receivedText = Color(theme.receivedTextColor)
    )

    val chatShapes = bubbleShapeFor(bubbleShapeOverride ?: theme.bubbleShape)

    val chatWallpaper = ChatWallpaper(
        type = theme.wallpaperType,
        value = theme.wallpaperValue,
        primaryColor = Color(theme.primaryColor),
        secondaryColor = Color(theme.secondaryColor)
    )

    CompositionLocalProvider(
        LocalChatColors provides chatColors,
        LocalChatShapes provides chatShapes,
        LocalChatWallpaper provides chatWallpaper,
        LocalActiveTheme provides theme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AuroraTypography,
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
