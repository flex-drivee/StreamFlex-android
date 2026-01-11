package com.streamflex.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflex.app.data.models.MediaDetails
import com.streamflex.app.data.models.StreamLink
import com.streamflex.app.data.providers.ProviderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailViewModel : ViewModel() {

    private val _details = MutableStateFlow<MediaDetails?>(null)
    val details = _details.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // ⚡ Load Description & Episode Buttons
    fun loadDetails(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val provider = ProviderManager.getAll().first()
                    val result = provider.loadDetails(url)
                    _details.value = result
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ⚡ Extract the final video link when user clicks a button
    fun loadStream(episodeUrl: String, callback: (List<StreamLink>) -> Unit) {
        viewModelScope.launch {
            val links = withContext(Dispatchers.IO) {
                val provider = ProviderManager.getAll().first()
                provider.loadLinks(episodeUrl)
            }
            callback(links)
        }
    }
}