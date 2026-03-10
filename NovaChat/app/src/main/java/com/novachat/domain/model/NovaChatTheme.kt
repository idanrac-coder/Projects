package com.novachat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NovaChatTheme(
    val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean = true,
    val isPremium: Boolean = false,
    val primaryColor: Long,
    val secondaryColor: Long,
    val surfaceColor: Long,
    val backgroundColor: Long,
    val sentBubbleColor: Long,
    val receivedBubbleColor: Long,
    val sentTextColor: Long,
    val receivedTextColor: Long,
    val bubbleShape: BubbleShape = BubbleShape.ROUNDED,
    val wallpaperType: WallpaperType = WallpaperType.SOLID,
    val wallpaperValue: String = "",
    val fontFamily: String = "default"
)

@Serializable
enum class BubbleShape {
    ROUNDED,
    SQUARE,
    CLOUD,
    MINIMAL,
    COMIC
}

@Serializable
enum class WallpaperType {
    SOLID,
    GRADIENT,
    IMAGE,
    PATTERN_BOTANICAL,
    PATTERN_GEOMETRIC,
    PATTERN_WAVES,
    PATTERN_DOTS,
    PATTERN_LEAVES,
    PATTERN_HALFTONE
}
