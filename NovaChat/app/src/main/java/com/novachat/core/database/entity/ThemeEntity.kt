package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.model.WallpaperType

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean,
    val isPremium: Boolean,
    val primaryColor: Long,
    val secondaryColor: Long,
    val surfaceColor: Long,
    val backgroundColor: Long,
    val sentBubbleColor: Long,
    val receivedBubbleColor: Long,
    val sentTextColor: Long,
    val receivedTextColor: Long,
    val bubbleShape: String,
    val wallpaperType: String,
    val wallpaperValue: String,
    val fontFamily: String
) {
    fun toDomainModel() = NovaChatTheme(
        id = id,
        name = name,
        isBuiltIn = isBuiltIn,
        isPremium = isPremium,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        surfaceColor = surfaceColor,
        backgroundColor = backgroundColor,
        sentBubbleColor = sentBubbleColor,
        receivedBubbleColor = receivedBubbleColor,
        sentTextColor = sentTextColor,
        receivedTextColor = receivedTextColor,
        bubbleShape = BubbleShape.valueOf(bubbleShape),
        wallpaperType = WallpaperType.valueOf(wallpaperType),
        wallpaperValue = wallpaperValue,
        fontFamily = fontFamily
    )

    companion object {
        fun fromDomainModel(theme: NovaChatTheme) = ThemeEntity(
            id = theme.id,
            name = theme.name,
            isBuiltIn = theme.isBuiltIn,
            isPremium = theme.isPremium,
            primaryColor = theme.primaryColor,
            secondaryColor = theme.secondaryColor,
            surfaceColor = theme.surfaceColor,
            backgroundColor = theme.backgroundColor,
            sentBubbleColor = theme.sentBubbleColor,
            receivedBubbleColor = theme.receivedBubbleColor,
            sentTextColor = theme.sentTextColor,
            receivedTextColor = theme.receivedTextColor,
            bubbleShape = theme.bubbleShape.name,
            wallpaperType = theme.wallpaperType.name,
            wallpaperValue = theme.wallpaperValue,
            fontFamily = theme.fontFamily
        )
    }
}
