package com.novachat.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.ConversationBackground
import com.novachat.domain.model.BuiltInBackgrounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackgroundsUiState(
    val backgrounds: List<ConversationBackground> = BuiltInBackgrounds.all,
    val selectedId: String = "default",
    val isPremium: Boolean = false,
    val previewBackground: ConversationBackground? = null
)

@HiltViewModel
class BackgroundsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _previewBackground = MutableStateFlow<ConversationBackground?>(null)

    val uiState: StateFlow<BackgroundsUiState> = combine(
        preferencesRepository.conversationBackgroundId,
        preferencesRepository.isPremium,
        _previewBackground
    ) { id, isPremium, preview ->
        BackgroundsUiState(
            backgrounds = BuiltInBackgrounds.all,
            selectedId = id,
            isPremium = isPremium,
            previewBackground = preview
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundsUiState())

    fun showPreview(background: ConversationBackground) {
        _previewBackground.value = background
    }

    fun confirmPreview() {
        val bg = _previewBackground.value ?: return
        viewModelScope.launch {
            preferencesRepository.setConversationBackgroundId(bg.id)
            _previewBackground.value = null
        }
    }

    fun dismissPreview() {
        _previewBackground.value = null
    }

    fun selectBackground(background: ConversationBackground) {
        viewModelScope.launch {
            preferencesRepository.setConversationBackgroundId(background.id)
        }
    }
}
