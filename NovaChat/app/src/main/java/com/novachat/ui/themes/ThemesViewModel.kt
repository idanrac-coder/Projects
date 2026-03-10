package com.novachat.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemesUiState(
    val builtInThemes: List<NovaChatTheme> = emptyList(),
    val customThemes: List<NovaChatTheme> = emptyList(),
    val activeThemeId: Long = 1L,
    val isPremium: Boolean = false,
    val activeBubbleShape: BubbleShape = BubbleShape.ROUNDED
)

@HiltViewModel
class ThemesViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<ThemesUiState> = combine(
        themeRepository.getBuiltInThemes(),
        themeRepository.getCustomThemes(),
        preferencesRepository.activeThemeId,
        preferencesRepository.isPremium,
        preferencesRepository.activeBubbleShape
    ) { builtIn, custom, activeId, premium, bubbleShape ->
        ThemesUiState(
            builtInThemes = builtIn,
            customThemes = custom,
            activeThemeId = activeId,
            isPremium = premium,
            activeBubbleShape = bubbleShape ?: BubbleShape.ROUNDED
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemesUiState())

    init {
        viewModelScope.launch {
            themeRepository.seedBuiltInThemes()
        }
    }

    fun applyTheme(themeId: Long) {
        viewModelScope.launch {
            preferencesRepository.setActiveThemeId(themeId)
        }
    }

    fun setBubbleShape(shape: BubbleShape) {
        viewModelScope.launch {
            preferencesRepository.setBubbleShape(shape)
            if (shape == BubbleShape.COMIC) {
                val comicTheme = themeRepository.getThemeByBubbleShape(BubbleShape.COMIC)
                comicTheme?.let {
                    preferencesRepository.setActiveThemeId(it.id)
                }
            }
        }
    }

    fun deleteCustomTheme(id: Long) {
        viewModelScope.launch {
            themeRepository.deleteCustomTheme(id)
        }
    }
}
