package com.streamflex.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflex.app.data.models.Media
import com.streamflex.app.data.providers.ProviderManager
import kotlinx.coroutines.Dispatchers // ⚡ Import this
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // ⚡ Import this

class HomeViewModel : ViewModel() {

    private val _movies = MutableStateFlow<List<Media>>(emptyList())
    val movies = _movies.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadHomePage()
    }

    fun loadHomePage() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // ⚡ SWITCH TO IO THREAD FOR NETWORK CALLS
                withContext(Dispatchers.IO) {
                    val provider = ProviderManager.getAll().firstOrNull()
                    if (provider != null) {
                        println("StreamFlex: Loading from ${provider.name}")
                        val results = provider.loadHome()

                        // ⚡ Switch back to Main Thread to update UI
                        _movies.value = results
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}