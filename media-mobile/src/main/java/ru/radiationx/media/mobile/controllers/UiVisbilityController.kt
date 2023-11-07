package ru.radiationx.media.mobile.controllers

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.media.mobile.PlayerFlow
import ru.radiationx.media.mobile.databinding.ViewPlayerBinding
import ru.radiationx.media.mobile.holder.PlayerAttachListener
import ru.radiationx.media.mobile.models.PlaybackState

internal class UiVisbilityController(
    private val coroutineScope: CoroutineScope,
    private val playerFlow: PlayerFlow,
    private val binding: ViewPlayerBinding,
) : PlayerAttachListener {

    companion object {
        private const val CONTROLS_HIDE_DELAY = 2000L
    }

    private var tapJob: Job? = null

    private val _internalState = MutableStateFlow(InternalState())

    private val _state = MutableStateFlow(UiVisibilityState())

    init {
        var overlayTransition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_SEQUENTIAL
            addTransition(Fade(Fade.OUT).apply {
                duration = 150
            })
            addTransition(ChangeBounds().apply {
                duration = 200
            })
            addTransition(Fade(Fade.IN).apply {
                duration = 150
            })
        }

        overlayTransition = AutoTransition().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            duration = 200
        }

        val rootTransition = AutoTransition().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            duration = 200
            addTarget(binding.mediaScrim)
            addTarget(binding.mediaScaleStroke)
        }

        combine(
            _internalState,
            playerFlow.playerState
        ) { internalState, playerState ->
            val hasError = playerState.errorMessage != null
            val seekerVisible =
                (internalState.scrollSeeker || internalState.doubleTapSeeker) && !internalState.liveScale
            val mainVisible =
                (internalState.main || internalState.slider) && !internalState.liveScale
            _state.value = UiVisibilityState(
                mainVisible = mainVisible,
                controlsVisible = !seekerVisible && mainVisible && !hasError,
                seekerVisible = seekerVisible,
                loadingVisible = playerState.isBlockingLoading,
                skipVisible = internalState.skip && !internalState.liveScale,
                errorVisible = !seekerVisible && hasError,
                liveScaleVisible = internalState.liveScale
            )
        }.launchIn(coroutineScope)

        _state.onEach {
            TransitionManager.beginDelayedTransition(binding.mediaOverlay, overlayTransition)
            //TransitionManager.beginDelayedTransition(binding.root as ViewGroup, rootTransition)

            binding.mediaButtonsContainer.isVisible = it.controlsVisible
            binding.mediaFooter.isVisible = it.mainVisible
            binding.mediaLoading.isVisible = it.loadingVisible
            binding.mediaSeekerTime.isVisible = it.seekerVisible
            binding.mediaScrim.isVisible = it.mainVisible
            binding.mediaSkipContainer.isVisible = it.skipVisible
            binding.mediaErrorContainer.isVisible = it.errorVisible
            binding.mediaScaleStroke.isVisible = it.liveScaleVisible
        }.launchIn(coroutineScope)
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

    fun updateSkip(active: Boolean) {
        _internalState.update { it.copy(skip = active) }
    }

    fun updateLiveScale(active: Boolean) {
        _internalState.update { it.copy(liveScale = active) }
    }

    private fun startDelayedHideControls() {
        tapJob?.cancel()
        setMainState(true)
        tapJob = coroutineScope.launch {
            val needsHide = playerFlow.playerState.value.let {
                it.playWhenReady && it.playbackState != PlaybackState.ENDED
            }
            delay(CONTROLS_HIDE_DELAY)
            if (needsHide) {
                setMainState(false)
            }
        }
    }

    private fun setMainState(state: Boolean) {
        _internalState.update { it.copy(main = state) }
    }

    private data class InternalState(
        val main: Boolean = false,
        val doubleTapSeeker: Boolean = false,
        val scrollSeeker: Boolean = false,
        val slider: Boolean = false,
        val skip: Boolean = false,
        val liveScale: Boolean = false,
    )

    private data class UiVisibilityState(
        val mainVisible: Boolean = false,
        val controlsVisible: Boolean = false,
        val seekerVisible: Boolean = false,
        val loadingVisible: Boolean = false,
        val skipVisible: Boolean = false,
        val errorVisible: Boolean = false,
        val liveScaleVisible: Boolean = false,
    )
}
