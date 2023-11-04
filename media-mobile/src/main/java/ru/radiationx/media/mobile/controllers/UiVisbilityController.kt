package ru.radiationx.media.mobile.controllers

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.media.mobile.RootPlayerHolder

class UiVisbilityController(
    private val holder: RootPlayerHolder,
) {

    companion object {
        private const val CONTROLS_HIDE_DELAY = 3000L
        private const val LIVE_SEEK_HIDE_DELAY = 500L
    }

    private var tapJob: Job? = null
    private var liveSeekJob: Job? = null

    private val _state = MutableStateFlow(UiVisibilityState())
    val state = _state.asStateFlow()

    init {

    }

    fun showControls() {
        startDelayedHideControls()
    }

    fun onSingleTap() {
        tapJob?.cancel()
        if (_state.value.controlsVisible) {
            _state.update { it.copy(controlsVisible = false) }
            return
        }
        startDelayedHideControls()
    }

    fun onSwipe() {
        onLiveSeek()
    }

    fun onDoubleTap() {
        onLiveSeek()
    }

    private fun startDelayedHideControls() {
        tapJob?.cancel()
        _state.update { it.copy(controlsVisible = true) }
        tapJob = holder.coroutineScope.launch {
            delay(CONTROLS_HIDE_DELAY)
            if (holder.flow.playerState.value.isPlaying) {
                _state.update { it.copy(controlsVisible = false) }
            }
        }
    }

    private fun onLiveSeek() {
        liveSeekJob?.cancel()
        _state.update { it.copy(liveSeekVisible = true) }
        tapJob = holder.coroutineScope.launch {
            delay(LIVE_SEEK_HIDE_DELAY)
            _state.update { it.copy(liveSeekVisible = false) }
        }
    }
}

data class UiVisibilityState(
    val controlsVisible: Boolean = false,
    val liveSeekVisible: Boolean = false,
) {
    val anyVisible = controlsVisible && liveSeekVisible
}