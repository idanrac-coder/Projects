package com.novachat.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.ConversationBackground
import com.novachat.domain.model.BuiltInBackgrounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackgroundsUiState(
    val backgrounds: List<ConversationBackground> = BuiltInBackgrounds.all,
    val selectedId: String = "default"
)

@HiltViewModel
class BackgroundsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<BackgroundsUiState> = preferencesRepository.conversationBackgroundId
        .map { id ->
            BackgroundsUiState(
                backgrounds = BuiltInBackgrounds.all,
                selectedId = id
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundsUiState())

    fun selectBackground(background: ConversationBackground) {
        viewModelScope.launch {
            preferencesRepository.setConversationBackgroundId(background.id)
        }
    }
}
