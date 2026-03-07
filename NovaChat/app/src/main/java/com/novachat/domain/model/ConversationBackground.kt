package com.novachat.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Preset backgrounds for chat conversations. Based on 2025-2026 messaging app best practices:
 * solid colors, soft gradients, subtle patterns, and wallpaper images for optimal readability.
 */
data class ConversationBackground(
    val id: String,
    val displayName: String,
    val type: WallpaperType,
    val primaryColor: Long,
    val secondaryColor: Long,
    val imageResName: String? = null
) {
    val primaryColorCompose: Color get() = Color(primaryColor)
    val secondaryColorCompose: Color get() = Color(secondaryColor)
}

object BuiltInBackgrounds {
    val all = listOf(
        // Default – follows active theme
        ConversationBackground(
            id = "default",
            displayName = "Default (Theme)",
            type = WallpaperType.SOLID,
            primaryColor = 0x00000000,
            secondaryColor = 0x00000000
        ),
        // Sky & nature gradients (2025 trending)
        ConversationBackground(
            id = "sky_blue",
            displayName = "Sky Blue",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFE3F2FD,
            secondaryColor = 0xFFBBDEFB
        ),
        ConversationBackground(
            id = "sunset",
            displayName = "Sunset",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFFFF3E0,
            secondaryColor = 0xFFFBE9E7
        ),
        ConversationBackground(
            id = "ocean",
            displayName = "Ocean",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFE0F7FA,
            secondaryColor = 0xFFB2EBF2
        ),
        ConversationBackground(
            id = "aurora",
            displayName = "Aurora",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFE8F5E9,
            secondaryColor = 0xFFE1BEE7
        ),
        ConversationBackground(
            id = "midnight",
            displayName = "Midnight",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFF1A237E,
            secondaryColor = 0xFF0D1B3A
        ),
        ConversationBackground(
            id = "forest",
            displayName = "Forest",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFE8F5E9,
            secondaryColor = 0xFFC8E6C9
        ),
        ConversationBackground(
            id = "lavender",
            displayName = "Lavender",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFF3E5F5,
            secondaryColor = 0xFFE1BEE7
        ),
        // Minimal solids (high readability)
        ConversationBackground(
            id = "minimal_white",
            displayName = "Minimal White",
            type = WallpaperType.SOLID,
            primaryColor = 0xFFFFFFFF,
            secondaryColor = 0xFFFFFFFF
        ),
        ConversationBackground(
            id = "minimal_gray",
            displayName = "Minimal Gray",
            type = WallpaperType.SOLID,
            primaryColor = 0xFFF5F5F5,
            secondaryColor = 0xFFF5F5F5
        ),
        ConversationBackground(
            id = "warm_sand",
            displayName = "Warm Sand",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFFFF8E1,
            secondaryColor = 0xFFFFECB3
        ),
        ConversationBackground(
            id = "coral",
            displayName = "Coral",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFFFF5F5,
            secondaryColor = 0xFFFFCCBC
        ),
        ConversationBackground(
            id = "mint",
            displayName = "Mint",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFE8F5E9,
            secondaryColor = 0xFFDCEDC8
        ),
        ConversationBackground(
            id = "storm",
            displayName = "Storm",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFECEFF1,
            secondaryColor = 0xFFCFD8DC
        ),
        ConversationBackground(
            id = "twilight",
            displayName = "Twilight",
            type = WallpaperType.GRADIENT,
            primaryColor = 0xFFEDE7F6,
            secondaryColor = 0xFFD1C4E9
        ),
        // Image backgrounds (2025–2026 messaging app styles: floral, geometric, nature, abstract)
        ConversationBackground(
            id = "img_floral",
            displayName = "Floral",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFFFF5F8,
            secondaryColor = 0xFFFFE4EC,
            imageResName = "bg_chat_floral"
        ),
        ConversationBackground(
            id = "img_geometric",
            displayName = "Geometric",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFF5F5FF,
            secondaryColor = 0xFFE8E8F5,
            imageResName = "bg_chat_geometric"
        ),
        ConversationBackground(
            id = "img_leaves",
            displayName = "Leaves",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFF0F8F0,
            secondaryColor = 0xFFE0F0E0,
            imageResName = "bg_chat_leaves"
        ),
        ConversationBackground(
            id = "img_dots",
            displayName = "Dots",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFFFFBF0,
            secondaryColor = 0xFFFFF0E0,
            imageResName = "bg_chat_dots"
        ),
        ConversationBackground(
            id = "img_waves",
            displayName = "Waves",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFF0F8FF,
            secondaryColor = 0xFFE0EFFF,
            imageResName = "bg_chat_waves"
        ),
        ConversationBackground(
            id = "img_abstract",
            displayName = "Abstract",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFF8F5FF,
            secondaryColor = 0xFFEDE8FA,
            imageResName = "bg_chat_abstract"
        ),
        ConversationBackground(
            id = "img_minimal",
            displayName = "Minimal Lines",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFFFFCF8,
            secondaryColor = 0xFFF5F2EE,
            imageResName = "bg_chat_minimal"
        ),
        ConversationBackground(
            id = "img_nature",
            displayName = "Nature",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFF5F8F0,
            secondaryColor = 0xFFE8F0E0,
            imageResName = "bg_chat_nature"
        ),
        ConversationBackground(
            id = "img_marble",
            displayName = "Marble",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFFF8F8F8,
            secondaryColor = 0xFFEEEEEE,
            imageResName = "bg_chat_marble"
        ),
        ConversationBackground(
            id = "img_stars",
            displayName = "Stars",
            type = WallpaperType.IMAGE,
            primaryColor = 0xFF0D1B2A,
            secondaryColor = 0xFF1A2744,
            imageResName = "bg_chat_stars"
        )
    )

    fun findById(id: String): ConversationBackground? = all.find { it.id == id }

    val imageBackgrounds: List<ConversationBackground> get() = all.filter { it.type == WallpaperType.IMAGE }

    val default: ConversationBackground get() = all.first()
}
