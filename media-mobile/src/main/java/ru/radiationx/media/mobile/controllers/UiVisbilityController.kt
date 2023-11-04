package ru.radiationx.media.mobile.controllers

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.media.mobile.RootPlayerHolder

class UiVisbilityController(
    private val holder: RootPlayerHolder,
) {

    companion object {
        private const val CONTROLS_HIDE_DELAY = 2000L
        private const val LIVE_SEEK_HIDE_DELAY = 500L
    }

    private var tapJob: Job? = null
    private var liveSeekJob: Job? = null

    private val _doubleTapSeekerState = MutableStateFlow(false)
    private val _scrollSeekerState = MutableStateFlow(false)

    private val _state = MutableStateFlow(UiVisibilityState())
    val state = _state.asStateFlow()

    init {
        holder.flow.playerState.onEach { playerState ->
            _state.update { it.copy(loadingVisible = playerState.isBlockingLoading) }
        }.launchIn(holder.coroutineScope)

        combine(
            _doubleTapSeekerState,
            _scrollSeekerState
        ) { doubleTap, scroll ->
            _state.update { it.copy(liveSeekVisible = doubleTap || scroll) }
        }.launchIn(holder.coroutineScope)
    }

    fun showControls() {
        startDelayedHideControls()
    }

    fun showControlsLocked() {
        tapJob?.cancel()
        _state.update { it.copy(controlsVisible = true) }
    }

    fun onSingleTap() {
        tapJob?.cancel()
        if (_state.value.controlsVisible) {
            _state.update { it.copy(controlsVisible = false) }
            return
        }
        startDelayedHideControls()
    }

    fun updateDoubleTapSeeker(active: Boolean) {
        _doubleTapSeekerState.value = active
    }

    fun updateScrollSeeker(active: Boolean) {
        _scrollSeekerState.value = active
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
}

data class UiVisibilityState(
    val controlsVisible: Boolean = false,
    val liveSeekVisible: Boolean = false,
    val loadingVisible: Boolean = false,
) {
    val anyVisible = controlsVisible && liveSeekVisible
}