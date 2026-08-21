package com.streamflex.app.ui.seeall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.domain.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SeeAllUiState(
    val title: String = "",
    val items: List<SearchResult> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class SeeAllViewModel(
    private val repository: ContentRepository,
    private val categoryId: String,
    private val initialTitle: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeeAllUiState(title = initialTitle))
    val uiState: StateFlow<SeeAllUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val results = repository.getCategory(categoryId, page = 1)
                _uiState.value = _uiState.value.copy(isLoading = false, items = results)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }
}

class SeeAllViewModelFactory(
    private val repository: ContentRepository,
    private val categoryId: String,
    private val title: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeeAllViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SeeAllViewModel(repository, categoryId, title) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
