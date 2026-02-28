package com.novachat.core.theme

import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.model.WallpaperType

object BuiltInThemes {

    val all = listOf(
        // FREE THEMES (3)
        NovaChatTheme(
            name = "Nova Default",
            isBuiltIn = true, isPremium = false,
            primaryColor = 0xFF6750A4, secondaryColor = 0xFF625B71,
            surfaceColor = 0xFFFFFBFE, backgroundColor = 0xFFFFFBFE,
            sentBubbleColor = 0xFF6750A4, receivedBubbleColor = 0xFFE8DEF8,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF1D1B20,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Midnight",
            isBuiltIn = true, isPremium = false,
            primaryColor = 0xFF1A1A2E, secondaryColor = 0xFF16213E,
            surfaceColor = 0xFF0F3460, backgroundColor = 0xFF1A1A2E,
            sentBubbleColor = 0xFFE94560, receivedBubbleColor = 0xFF16213E,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFFE0E0E0,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Ocean Breeze",
            isBuiltIn = true, isPremium = false,
            primaryColor = 0xFF0077B6, secondaryColor = 0xFF00B4D8,
            surfaceColor = 0xFFCAF0F8, backgroundColor = 0xFFEDF6F9,
            sentBubbleColor = 0xFF0077B6, receivedBubbleColor = 0xFFCAF0F8,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF023E8A,
            bubbleShape = BubbleShape.ROUNDED
        ),

        // PREMIUM THEMES
        NovaChatTheme(
            name = "Sunset Glow",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFFF6B6B, secondaryColor = 0xFFFFA07A,
            surfaceColor = 0xFFFFF5EE, backgroundColor = 0xFFFFF0E5,
            sentBubbleColor = 0xFFFF6B6B, receivedBubbleColor = 0xFFFFE4C4,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF8B4513,
            bubbleShape = BubbleShape.CLOUD
        ),
        NovaChatTheme(
            name = "Forest",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF2D6A4F, secondaryColor = 0xFF40916C,
            surfaceColor = 0xFFD8F3DC, backgroundColor = 0xFFB7E4C7,
            sentBubbleColor = 0xFF2D6A4F, receivedBubbleColor = 0xFFD8F3DC,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF1B4332,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Lavender Dream",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF7B2D8E, secondaryColor = 0xFFAB47BC,
            surfaceColor = 0xFFF3E5F5, backgroundColor = 0xFFEDE7F6,
            sentBubbleColor = 0xFF7B2D8E, receivedBubbleColor = 0xFFE1BEE7,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF4A148C,
            bubbleShape = BubbleShape.CLOUD
        ),
        NovaChatTheme(
            name = "Cherry Blossom",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFE91E63, secondaryColor = 0xFFF48FB1,
            surfaceColor = 0xFFFCE4EC, backgroundColor = 0xFFFFF0F5,
            sentBubbleColor = 0xFFE91E63, receivedBubbleColor = 0xFFF8BBD0,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF880E4F,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Arctic",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF37474F, secondaryColor = 0xFF546E7A,
            surfaceColor = 0xFFECEFF1, backgroundColor = 0xFFF5F5F5,
            sentBubbleColor = 0xFF37474F, receivedBubbleColor = 0xFFCFD8DC,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF263238,
            bubbleShape = BubbleShape.SQUARE
        ),
        NovaChatTheme(
            name = "Golden Hour",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFFF8F00, secondaryColor = 0xFFFFC107,
            surfaceColor = 0xFFFFF8E1, backgroundColor = 0xFFFFFDE7,
            sentBubbleColor = 0xFFFF8F00, receivedBubbleColor = 0xFFFFECB3,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFFE65100,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Neon Nights",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF00E5FF, secondaryColor = 0xFFFF1744,
            surfaceColor = 0xFF121212, backgroundColor = 0xFF0D0D0D,
            sentBubbleColor = 0xFF00E5FF, receivedBubbleColor = 0xFF1E1E1E,
            sentTextColor = 0xFF000000, receivedTextColor = 0xFF00E5FF,
            bubbleShape = BubbleShape.MINIMAL
        ),
        NovaChatTheme(
            name = "Rose Gold",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFB76E79, secondaryColor = 0xFFC9A0A0,
            surfaceColor = 0xFFFFF5F5, backgroundColor = 0xFFFFF0F0,
            sentBubbleColor = 0xFFB76E79, receivedBubbleColor = 0xFFFFE4E4,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF6D3A3A,
            bubbleShape = BubbleShape.CLOUD
        ),
        NovaChatTheme(
            name = "Cyberpunk",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFFF00FF, secondaryColor = 0xFF00FFFF,
            surfaceColor = 0xFF1A0A2E, backgroundColor = 0xFF0D0221,
            sentBubbleColor = 0xFFFF00FF, receivedBubbleColor = 0xFF2D1B69,
            sentTextColor = 0xFF000000, receivedTextColor = 0xFF00FFFF,
            bubbleShape = BubbleShape.SQUARE
        ),
        NovaChatTheme(
            name = "Mint Fresh",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF00BFA5, secondaryColor = 0xFF64FFDA,
            surfaceColor = 0xFFE0F2F1, backgroundColor = 0xFFF0FFF4,
            sentBubbleColor = 0xFF00BFA5, receivedBubbleColor = 0xFFB2DFDB,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF004D40,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Mocha",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF795548, secondaryColor = 0xFFA1887F,
            surfaceColor = 0xFFEFEBE9, backgroundColor = 0xFFF5F0EB,
            sentBubbleColor = 0xFF795548, receivedBubbleColor = 0xFFD7CCC8,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF3E2723,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Coral Reef",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFFF7043, secondaryColor = 0xFFFF8A65,
            surfaceColor = 0xFFFBE9E7, backgroundColor = 0xFFFFF3E0,
            sentBubbleColor = 0xFFFF7043, receivedBubbleColor = 0xFFFFCCBC,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFFBF360C,
            bubbleShape = BubbleShape.CLOUD
        ),
        NovaChatTheme(
            name = "AMOLED Dark",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF448AFF, secondaryColor = 0xFF82B1FF,
            surfaceColor = 0xFF000000, backgroundColor = 0xFF000000,
            sentBubbleColor = 0xFF448AFF, receivedBubbleColor = 0xFF1A1A1A,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFFE0E0E0,
            bubbleShape = BubbleShape.MINIMAL
        ),
        NovaChatTheme(
            name = "Pastel Rainbow",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFAED581, secondaryColor = 0xFF81D4FA,
            surfaceColor = 0xFFFFF9C4, backgroundColor = 0xFFFFFDE7,
            sentBubbleColor = 0xFFAED581, receivedBubbleColor = 0xFFF8BBD0,
            sentTextColor = 0xFF33691E, receivedTextColor = 0xFF880E4F,
            bubbleShape = BubbleShape.CLOUD
        ),
        NovaChatTheme(
            name = "Sapphire",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF1565C0, secondaryColor = 0xFF42A5F5,
            surfaceColor = 0xFFE3F2FD, backgroundColor = 0xFFE8EAF6,
            sentBubbleColor = 0xFF1565C0, receivedBubbleColor = 0xFFBBDEFB,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF0D47A1,
            bubbleShape = BubbleShape.ROUNDED
        )
    )
}
