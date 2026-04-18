package com.novachat.ui.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.DEFAULT_USER_CATEGORIES
import com.novachat.domain.model.UserCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val categories: StateFlow<List<UserCategory>> = userPreferencesRepository.userCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_USER_CATEGORIES)

    fun saveCategory(category: UserCategory) {
        viewModelScope.launch { userPreferencesRepository.saveUserCategory(category) }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch { userPreferencesRepository.deleteUserCategory(id) }
    }

    fun addCustomCategory(displayName: String, colorHex: String) {
        val newCat = UserCategory(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            colorHex = colorHex,
            isBuiltIn = false
        )
        viewModelScope.launch { userPreferencesRepository.saveUserCategory(newCat) }
    }
}
