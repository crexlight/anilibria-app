package ru.radiationx.media.mobile

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.controllers.MediaButtonsController
import ru.radiationx.media.mobile.controllers.OutputController
import ru.radiationx.media.mobile.controllers.SkipsController
import ru.radiationx.media.mobile.controllers.TimelineController
import ru.radiationx.media.mobile.controllers.UiVisbilityController
import ru.radiationx.media.mobile.controllers.gesture.GestureController
import ru.radiationx.media.mobile.databinding.ViewPlayerBinding
import ru.radiationx.media.mobile.holder.RootPlayerHolder

class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val binding by viewBinding<ViewPlayerBinding>(attachToRoot = true)

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val playerFlow = PlayerFlow(coroutineScope)
    private val holder = RootPlayerHolder()

    private val outputController = OutputController(
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        mediaTextureView = binding.mediaTextureView,
        mediaAspectRatio = binding.mediaAspectRatio
    )

    private val uiVisbilityController = UiVisbilityController(
        coroutineScope = coroutineScope,
        playerFlow = playerFlow
    )

    private val mediaButtonsController = MediaButtonsController(
        holder = holder,
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        mediaButtonPrev = binding.mediaButtonPrev,
        mediaButtonPlay = binding.mediaButtonPlay,
        mediaButtonNext = binding.mediaButtonNext
    )

    private val timelineController = TimelineController(
        holder = holder,
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        slider = binding.mediaSlider,
        bufferingSlider = binding.mediaBufferingSlider,
        mediaTime = binding.mediaTime
    )

    private val gestureController = GestureController(
        holder = holder,
        coroutineScope = coroutineScope,
        gestureView = binding.mediaOverlay,
        seekerTime = binding.mediaSeekerTime
    )

    private val skipsController = SkipsController(
        holder = holder,
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        skipButtonCancel = binding.mediaSkipButtonCancel,
        skipButtonSkip = binding.mediaSkipButtonSkip
    )

    init {
        holder.addListener(playerFlow)
        holder.addListener(outputController)
        holder.addListener(uiVisbilityController)
        holder.addListener(mediaButtonsController)
        holder.addListener(timelineController)
        holder.addListener(gestureController)
        holder.addListener(skipsController)

        uiVisbilityController.state.onEach {
            Log.d("kekeke", "$it")
            TransitionManager.beginDelayedTransition(
                binding.mediaOverlay,
                AutoTransition().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    duration = 200L
                }
            )

            binding.mediaButtonsContainer.isVisible = it.controlsVisible
            binding.mediaFooter.isVisible = it.mainVisible
            binding.mediaLoading.isVisible = it.loadingVisible
            binding.mediaSeekerTime.isVisible = it.seekerVisible
            binding.mediaScrim.isVisible = it.mainVisible
            binding.mediaSkipContainer.isVisible = it.skipVisible
        }.launchIn(coroutineScope)

        mediaButtonsController.onAnyTap = {
            uiVisbilityController.showMain()
        }

        gestureController.singleTapListener = {
            uiVisbilityController.toggleMainVisible()
        }

        gestureController.doubleTapSeekerState.onEach {
            uiVisbilityController.updateDoubleTapSeeker(it.isActive)
        }.launchIn(coroutineScope)

        gestureController.scrollSeekerState.onEach {
            uiVisbilityController.updateScrollSeeker(it.isActive)
        }.launchIn(coroutineScope)

        timelineController.seekState.onEach {
            uiVisbilityController.updateSlider(it != null)
        }.launchIn(coroutineScope)

        skipsController.currentSkip.onEach {
            uiVisbilityController.updateSkip(it != null)
        }.launchIn(coroutineScope)

    }

    fun setPlayer(player: Player?) {
        holder.setPlayer(player)
    }


}