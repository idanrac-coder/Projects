package com.novachat.ui.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.SwipeAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SwipeActionsUiState(
    val leftAction: SwipeAction = SwipeAction.ARCHIVE,
    val rightAction: SwipeAction = SwipeAction.DELETE
)

@HiltViewModel
class SwipeActionsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SwipeActionsUiState> = combine(
        preferencesRepository.swipeLeftAction,
        preferencesRepository.swipeRightAction
    ) { left, right ->
        SwipeActionsUiState(leftAction = left, rightAction = right)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeActionsUiState())

    fun setLeftAction(action: SwipeAction) {
        viewModelScope.launch {
            preferencesRepository.setSwipeLeftAction(action)
        }
    }

    fun setRightAction(action: SwipeAction) {
        viewModelScope.launch {
            preferencesRepository.setSwipeRightAction(action)
        }
    }
}
