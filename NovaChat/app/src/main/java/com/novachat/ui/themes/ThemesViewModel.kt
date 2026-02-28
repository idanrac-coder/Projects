package com.novachat.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    val selectedTab: Int = 0
)

@HiltViewModel
class ThemesViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<ThemesUiState> = combine(
        themeRepository.getBuiltInThemes(),
        themeRepository.getCustomThemes(),
        preferencesRepository.activeThemeId,
        preferencesRepository.isPremium,
        _selectedTab
    ) { builtIn, custom, activeId, premium, tab ->
        ThemesUiState(
            builtInThemes = builtIn,
            customThemes = custom,
            activeThemeId = activeId,
            isPremium = premium,
            selectedTab = tab
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemesUiState())

    init {
        viewModelScope.launch {
            themeRepository.seedBuiltInThemes()
        }
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun applyTheme(themeId: Long) {
        viewModelScope.launch {
            preferencesRepository.setActiveThemeId(themeId)
        }
    }

    fun deleteCustomTheme(id: Long) {
        viewModelScope.launch {
            themeRepository.deleteCustomTheme(id)
        }
    }
}
