package com.novachat.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.model.WallpaperType
import com.novachat.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeEditorViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _theme = MutableStateFlow(
        NovaChatTheme(
            name = "My Custom Theme",
            isBuiltIn = false,
            isPremium = false,
            primaryColor = 0xFF6750A4,
            secondaryColor = 0xFF625B71,
            surfaceColor = 0xFFFFFBFE,
            backgroundColor = 0xFFFFFBFE,
            sentBubbleColor = 0xFF6750A4,
            receivedBubbleColor = 0xFFE8DEF8,
            sentTextColor = 0xFFFFFFFF,
            receivedTextColor = 0xFF1D1B20,
            bubbleShape = BubbleShape.ROUNDED,
            wallpaperType = WallpaperType.SOLID,
            wallpaperValue = "",
            fontFamily = "default"
        )
    )
    val theme: StateFlow<NovaChatTheme> = _theme.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun updateName(name: String) {
        _theme.value = _theme.value.copy(name = name)
    }

    fun updatePrimaryColor(color: Long) {
        _theme.value = _theme.value.copy(primaryColor = color)
    }

    fun updateSecondaryColor(color: Long) {
        _theme.value = _theme.value.copy(secondaryColor = color)
    }

    fun updateBackgroundColor(color: Long) {
        _theme.value = _theme.value.copy(backgroundColor = color)
    }

    fun updateSentBubbleColor(color: Long) {
        _theme.value = _theme.value.copy(sentBubbleColor = color)
    }

    fun updateReceivedBubbleColor(color: Long) {
        _theme.value = _theme.value.copy(receivedBubbleColor = color)
    }

    fun updateSentTextColor(color: Long) {
        _theme.value = _theme.value.copy(sentTextColor = color)
    }

    fun updateReceivedTextColor(color: Long) {
        _theme.value = _theme.value.copy(receivedTextColor = color)
    }

    fun updateSurfaceColor(color: Long) {
        _theme.value = _theme.value.copy(surfaceColor = color)
    }

    fun updateBubbleShape(shape: BubbleShape) {
        _theme.value = _theme.value.copy(bubbleShape = shape)
    }

    fun saveTheme() {
        viewModelScope.launch {
            themeRepository.saveTheme(_theme.value)
            _saved.value = true
        }
    }
}
