package com.novachat.domain.model

data class NotificationSettings(
    val isEnabled: Boolean = true,
    val soundUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val vibrationPattern: LongArray? = null,
    val ledColor: Int? = null,
    val popupStyle: PopupStyle = PopupStyle.HEADS_UP,
    val priorityLevel: PriorityLevel = PriorityLevel.DEFAULT,
    val bubbleEnabled: Boolean = false,
    val groupingMode: GroupingMode = GroupingMode.BY_CONTACT
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationSettings) return false
        return isEnabled == other.isEnabled &&
            soundUri == other.soundUri &&
            vibrationEnabled == other.vibrationEnabled &&
            vibrationPattern.contentEquals(other.vibrationPattern) &&
            ledColor == other.ledColor &&
            popupStyle == other.popupStyle &&
            priorityLevel == other.priorityLevel &&
            bubbleEnabled == other.bubbleEnabled &&
            groupingMode == other.groupingMode
    }

    override fun hashCode(): Int {
        var result = isEnabled.hashCode()
        result = 31 * result + (soundUri?.hashCode() ?: 0)
        result = 31 * result + vibrationEnabled.hashCode()
        result = 31 * result + (vibrationPattern?.contentHashCode() ?: 0)
        result = 31 * result + (ledColor ?: 0)
        result = 31 * result + popupStyle.hashCode()
        result = 31 * result + priorityLevel.hashCode()
        result = 31 * result + bubbleEnabled.hashCode()
        result = 31 * result + groupingMode.hashCode()
        return result
    }
}

enum class PopupStyle { HEADS_UP, BANNER, SILENT }
enum class PriorityLevel { HIGH, DEFAULT, LOW }
enum class GroupingMode { BY_CONTACT, BUNDLE_ALL, INDIVIDUAL }
