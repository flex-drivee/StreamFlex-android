package com.streamflex.player.episodes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NextEpisodeManager(private val scope: CoroutineScope) {

    private val _showNextEpisodeCard = MutableStateFlow(false)
    val showNextEpisodeCard: StateFlow<Boolean> = _showNextEpisodeCard.asStateFlow()
    
    private val _countdownSeconds = MutableStateFlow(10)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()
    
    private var countdownJob: Job? = null
    
    fun triggerNextEpisodeCountdown(onCountdownComplete: () -> Unit) {
        _showNextEpisodeCard.value = true
        _countdownSeconds.value = 10
        
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (_countdownSeconds.value > 0) {
                delay(1000)
                _countdownSeconds.value -= 1
            }
            _showNextEpisodeCard.value = false
            onCountdownComplete()
        }
    }
    
    fun cancelCountdown() {
        countdownJob?.cancel()
        _showNextEpisodeCard.value = false
    }
}
