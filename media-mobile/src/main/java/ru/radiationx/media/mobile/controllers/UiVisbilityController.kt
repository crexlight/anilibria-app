package ru.radiationx.media.mobile.controllers

import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.media.mobile.RootPlayerHolder

class UiVisbilityController(
    private val holder: RootPlayerHolder,
) {

    companion object {
        private const val CONTROLS_HIDE_DELAY = 2000L
    }

    private var tapJob: Job? = null

    private val _internalState = MutableStateFlow(InternalState())

    private val _state = MutableStateFlow(UiVisibilityState())
    val state = _state.asStateFlow()

    init {
        combine(
            _internalState,
            holder.flow.playerState
        ) { internalState, playerState ->
            Log.d("kekeke", "$internalState")
            val seekerVisible = internalState.scrollSeeker || internalState.doubleTapSeeker
            val mainVisible = internalState.main || internalState.slider
            _state.value = UiVisibilityState(
                mainVisible = mainVisible,
                controlsVisible = !seekerVisible && mainVisible,
                seekerVisible = seekerVisible,
                loadingVisible = playerState.isBlockingLoading
            )
        }.launchIn(holder.coroutineScope)
    }

    fun showMain() {
        startDelayedHideControls()
    }

    fun toggleMainVisible() {
        tapJob?.cancel()
        if (_internalState.value.main) {
            setMainState(false)
            return
        }
        startDelayedHideControls()
    }

    fun updateDoubleTapSeeker(active: Boolean) {
        _internalState.update { it.copy(doubleTapSeeker = active) }
    }

    fun updateScrollSeeker(active: Boolean) {
        _internalState.update { it.copy(scrollSeeker = active) }
    }

    fun updateSlider(active: Boolean) {
        _internalState.update { it.copy(slider = active) }
    }

    private fun startDelayedHideControls() {
        tapJob?.cancel()
        setMainState(true)
        tapJob = holder.coroutineScope.launch {
            val needsHide = holder.flow.playerState.value.isPlaying
            delay(CONTROLS_HIDE_DELAY)
            if (needsHide) {
                setMainState(false)
            }
        }
    }

    private fun setMainState(state: Boolean) {
        _internalState.update { it.copy(main = state) }
    }

    data class InternalState(
        val main: Boolean = false,
        val doubleTapSeeker: Boolean = false,
        val scrollSeeker: Boolean = false,
        val slider: Boolean = false,
    )
}

data class UiVisibilityState(
    val mainVisible: Boolean = false,
    val controlsVisible: Boolean = false,
    val seekerVisible: Boolean = false,
    val loadingVisible: Boolean = false,
) {
    val anyVisible = controlsVisible && seekerVisible
}