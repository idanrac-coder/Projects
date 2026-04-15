package com.novachat.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.billing.LicenseManager
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
    val activeBubbleShape: BubbleShape = BubbleShape.ROUNDED,
    val themeMode: String = "system"
)

@HiltViewModel
class ThemesViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val licenseManager: LicenseManager
) : ViewModel() {

    val uiState: StateFlow<ThemesUiState> = combine(
        themeRepository.getBuiltInThemes(),
        themeRepository.getCustomThemes(),
        preferencesRepository.activeThemeId,
        licenseManager.hasPremiumAccess,
        combine(
            preferencesRepository.activeBubbleShape,
            preferencesRepository.themeMode
        ) { bubble, mode -> bubble to mode }
    ) { builtIn, custom, activeId, premium, (bubbleShape, themeMode) ->
        ThemesUiState(
            builtInThemes = builtIn,
            customThemes = custom,
            activeThemeId = activeId,
            isPremium = premium,
            activeBubbleShape = bubbleShape ?: BubbleShape.ROUNDED,
            themeMode = themeMode
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
            preferencesRepository.setThemeMode("custom")
        }
    }

    fun setBubbleShape(shape: BubbleShape) {
        viewModelScope.launch {
            preferencesRepository.setBubbleShape(shape)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun deleteCustomTheme(id: Long) {
        viewModelScope.launch {
            themeRepository.deleteCustomTheme(id)
        }
    }
}
