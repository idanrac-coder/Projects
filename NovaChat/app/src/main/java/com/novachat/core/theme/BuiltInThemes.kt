package com.novachat.core.theme

import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.model.WallpaperType

object BuiltInThemes {

    val all = listOf(
        // DEFAULT — Aurora "Nebula" theme
        NovaChatTheme(
            name = "Nebula",
            isBuiltIn = true, isPremium = false,
            primaryColor = 0xFF6C5CE7, secondaryColor = 0xFF00CEC9,
            surfaceColor = 0xFFF8F9FE, backgroundColor = 0xFFFFFFFF,
            sentBubbleColor = 0xFF6C5CE7, receivedBubbleColor = 0xFFF0EFFF,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF1A1B2E,
            bubbleShape = BubbleShape.ROUNDED
        ),

        // FREE THEMES
        NovaChatTheme(
            name = "Sage Garden",
            isBuiltIn = true, isPremium = false,
            primaryColor = 0xFF8B9C5C, secondaryColor = 0xFFA4AE74,
            surfaceColor = 0xFFF5F0E4, backgroundColor = 0xFFEDE8D5,
            sentBubbleColor = 0xFFD2D8A8, receivedBubbleColor = 0xFFF5F0E4,
            sentTextColor = 0xFF3A3D25, receivedTextColor = 0xFF3A3D25,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_BOTANICAL
        ),
        NovaChatTheme(
            name = "Midnight",
            isBuiltIn = true, isPremium = false,
            primaryColor = 0xFFA29BFE, secondaryColor = 0xFF55EFC4,
            surfaceColor = 0xFF1A1B2E, backgroundColor = 0xFF0F1023,
            sentBubbleColor = 0xFF6C5CE7, receivedBubbleColor = 0xFF232446,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFFE8E8F0,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Horizon",
            isBuiltIn = true, isPremium = false,
            primaryColor = 0xFF5B7B95, secondaryColor = 0xFF8AAEC0,
            surfaceColor = 0xFFF2F4F6, backgroundColor = 0xFFECEFF2,
            sentBubbleColor = 0xFF5B7B95, receivedBubbleColor = 0xFFF2F4F6,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF1E2D3A,
            bubbleShape = BubbleShape.MINIMAL,
            wallpaperType = WallpaperType.PATTERN_GEOMETRIC
        ),

        // PREMIUM THEMES — Earthy / Botanical
        NovaChatTheme(
            name = "Olive Grove",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF6B7B3A, secondaryColor = 0xFF8B9A56,
            surfaceColor = 0xFFF8F5EC, backgroundColor = 0xFFF3EFE2,
            sentBubbleColor = 0xFFCCD4A0, receivedBubbleColor = 0xFFEDE9DC,
            sentTextColor = 0xFF2E3518, receivedTextColor = 0xFF3A3D2A,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_LEAVES
        ),
        NovaChatTheme(
            name = "Eucalyptus",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF5E8B7E, secondaryColor = 0xFF7CAA9C,
            surfaceColor = 0xFFF0F5F3, backgroundColor = 0xFFE8F0ED,
            sentBubbleColor = 0xFFB8D4CC, receivedBubbleColor = 0xFFE8F0ED,
            sentTextColor = 0xFF1E3A32, receivedTextColor = 0xFF2A4238,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_BOTANICAL
        ),
        NovaChatTheme(
            name = "Terracotta",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFB56B4A, secondaryColor = 0xFFD4926E,
            surfaceColor = 0xFFFAF3ED, backgroundColor = 0xFFF5EDE5,
            sentBubbleColor = 0xFFE8C5A8, receivedBubbleColor = 0xFFFAF3ED,
            sentTextColor = 0xFF4A2A18, receivedTextColor = 0xFF4A3528,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_WAVES
        ),
        NovaChatTheme(
            name = "Lavender Fields",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF8878A8, secondaryColor = 0xFFA498C0,
            surfaceColor = 0xFFF4F0F8, backgroundColor = 0xFFECE8F2,
            sentBubbleColor = 0xFFCCC4DC, receivedBubbleColor = 0xFFF4F0F8,
            sentTextColor = 0xFF2E2840, receivedTextColor = 0xFF2E2840,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_BOTANICAL
        ),
        NovaChatTheme(
            name = "Dusty Rose",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFC08088, secondaryColor = 0xFFD0989E,
            surfaceColor = 0xFFFCF2F3, backgroundColor = 0xFFF8EAEC,
            sentBubbleColor = 0xFFE8C8CC, receivedBubbleColor = 0xFFFCF2F3,
            sentTextColor = 0xFF4A2028, receivedTextColor = 0xFF4A2028,
            bubbleShape = BubbleShape.CLOUD,
            wallpaperType = WallpaperType.PATTERN_DOTS
        ),

        // PREMIUM THEMES — Modern / Bold
        NovaChatTheme(
            name = "Ocean Breeze",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF0984E3, secondaryColor = 0xFF74B9FF,
            surfaceColor = 0xFFF0F6FC, backgroundColor = 0xFFFFFFFF,
            sentBubbleColor = 0xFF0984E3, receivedBubbleColor = 0xFFE8F2FC,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF1A2E40,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_WAVES
        ),
        NovaChatTheme(
            name = "AMOLED Dark",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF6C5CE7, secondaryColor = 0xFF00CEC9,
            surfaceColor = 0xFF0A0A0A, backgroundColor = 0xFF000000,
            sentBubbleColor = 0xFF6C5CE7, receivedBubbleColor = 0xFF1A1A1A,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFFB0B0B0,
            bubbleShape = BubbleShape.MINIMAL
        ),
        NovaChatTheme(
            name = "Sunset Glow",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFE17055, secondaryColor = 0xFFFAB1A0,
            surfaceColor = 0xFFFFF8F5, backgroundColor = 0xFFFFFFFF,
            sentBubbleColor = 0xFFE17055, receivedBubbleColor = 0xFFFFF0EC,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF4A2818,
            bubbleShape = BubbleShape.CLOUD,
            wallpaperType = WallpaperType.PATTERN_GEOMETRIC
        ),
        NovaChatTheme(
            name = "Cherry Blossom",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFE84393, secondaryColor = 0xFFFD79A8,
            surfaceColor = 0xFFFFF5F9, backgroundColor = 0xFFFFFFFF,
            sentBubbleColor = 0xFFE84393, receivedBubbleColor = 0xFFFFF0F5,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF4A1828,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_BOTANICAL
        ),
        NovaChatTheme(
            name = "Mocha",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF8C7060, secondaryColor = 0xFFA48878,
            surfaceColor = 0xFFF6F0EC, backgroundColor = 0xFFF0E8E2,
            sentBubbleColor = 0xFFD4C0B0, receivedBubbleColor = 0xFFF6F0EC,
            sentTextColor = 0xFF3A2820, receivedTextColor = 0xFF3A2820,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_DOTS
        ),
        NovaChatTheme(
            name = "Arctic Frost",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF6890A0, secondaryColor = 0xFF88A8B8,
            surfaceColor = 0xFFF0F4F6, backgroundColor = 0xFFE8EEF2,
            sentBubbleColor = 0xFFB8D0DC, receivedBubbleColor = 0xFFF0F4F6,
            sentTextColor = 0xFF1C3038, receivedTextColor = 0xFF1C3038,
            bubbleShape = BubbleShape.MINIMAL,
            wallpaperType = WallpaperType.PATTERN_GEOMETRIC
        ),
        NovaChatTheme(
            name = "Golden Hour",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFC0984C, secondaryColor = 0xFFD4AC68,
            surfaceColor = 0xFFFCF8EC, backgroundColor = 0xFFF8F0DE,
            sentBubbleColor = 0xFFE8D8A8, receivedBubbleColor = 0xFFFCF8EC,
            sentTextColor = 0xFF3A3010, receivedTextColor = 0xFF3A3010,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_WAVES
        ),
        NovaChatTheme(
            name = "Mint Fresh",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF00B894, secondaryColor = 0xFF55EFC4,
            surfaceColor = 0xFFF0FAF6, backgroundColor = 0xFFFFFFFF,
            sentBubbleColor = 0xFF00B894, receivedBubbleColor = 0xFFE8F8F2,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF183830,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.PATTERN_LEAVES
        ),

        // PREMIUM THEMES — New Aurora collection
        NovaChatTheme(
            name = "Deep Nebula",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF6C5CE7, secondaryColor = 0xFFFD79A8,
            surfaceColor = 0xFF1A1B2E, backgroundColor = 0xFF0F1023,
            sentBubbleColor = 0xFF6C5CE7, receivedBubbleColor = 0xFF232446,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFFE8E8F0,
            bubbleShape = BubbleShape.CLOUD
        ),
        NovaChatTheme(
            name = "Aurora Borealis",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF00CEC9, secondaryColor = 0xFF6C5CE7,
            surfaceColor = 0xFFF0FFFE, backgroundColor = 0xFFFFFFFF,
            sentBubbleColor = 0xFF00CEC9, receivedBubbleColor = 0xFFE8FFFE,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF1A2E2E,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Rose Quartz",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFFD79A8, secondaryColor = 0xFFFDCB6E,
            surfaceColor = 0xFFFFF5F9, backgroundColor = 0xFFFFFFFF,
            sentBubbleColor = 0xFFFD79A8, receivedBubbleColor = 0xFFFFF0F5,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFF3A1828,
            bubbleShape = BubbleShape.CLOUD
        ),
        NovaChatTheme(
            name = "Cosmic Night",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFF0984E3, secondaryColor = 0xFF6C5CE7,
            surfaceColor = 0xFF101828, backgroundColor = 0xFF0A1018,
            sentBubbleColor = 0xFF0984E3, receivedBubbleColor = 0xFF1A2438,
            sentTextColor = 0xFFFFFFFF, receivedTextColor = 0xFFD0D8E8,
            bubbleShape = BubbleShape.ROUNDED
        ),
        NovaChatTheme(
            name = "Amber Dusk",
            isBuiltIn = true, isPremium = true,
            primaryColor = 0xFFFDCB6E, secondaryColor = 0xFFE17055,
            surfaceColor = 0xFF1E1810, backgroundColor = 0xFF141008,
            sentBubbleColor = 0xFFFDCB6E, receivedBubbleColor = 0xFF2A2218,
            sentTextColor = 0xFF1A1008, receivedTextColor = 0xFFE8DCC8,
            bubbleShape = BubbleShape.MINIMAL
        )
    )
}
