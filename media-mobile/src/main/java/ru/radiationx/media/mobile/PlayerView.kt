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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.controllers.MediaButtonsController
import ru.radiationx.media.mobile.controllers.OutputController
import ru.radiationx.media.mobile.controllers.SkipsController
import ru.radiationx.media.mobile.controllers.TimelineController
import ru.radiationx.media.mobile.controllers.UiVisbilityController
import ru.radiationx.media.mobile.controllers.gesture.GestureController
import ru.radiationx.media.mobile.databinding.ViewPlayerBinding

class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val binding by viewBinding<ViewPlayerBinding>(attachToRoot = true)

    private val holder = RootPlayerHolder()

    private val outputController = OutputController(
        holder = holder,
        mediaTextureView = binding.mediaTextureView,
        mediaAspectRatio = binding.mediaAspectRatio
    )

    private val uiVisbilityController = UiVisbilityController(
        holder = holder
    )

    private val mediaButtonsController = MediaButtonsController(
        holder = holder,
        mediaButtonPrev = binding.mediaButtonPrev,
        mediaButtonPlay = binding.mediaButtonPlay,
        mediaButtonNext = binding.mediaButtonNext
    )

    private val timelineController = TimelineController(
        holder = holder,
        slider = binding.mediaSlider,
        bufferingSlider = binding.mediaBufferingSlider,
        mediaTime = binding.mediaTime
    )

    private val gestureController = GestureController(
        holder = holder,
        gestureView = binding.mediaControlsContainer,
        seekerTime = binding.mediaSeekerTime
    )

    private val skipsController = SkipsController(
        holder = holder,
        skipButtonCancel = binding.mediaSkipButtonCancel,
        skipButtonSkip = binding.mediaSkipButtonSkip
    )

    init {
        holder.flow.playerState.onEach {
            //Log.d("kekeke", "player state $it")
        }.launchIn(holder.coroutineScope)

        uiVisbilityController.state.onEach {
            Log.d("kekeke", "$it")
            TransitionManager.beginDelayedTransition(
                binding.mediaControlsContainer,
                AutoTransition().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    duration = 200L
                }
            )

            binding.mediaControls.isVisible = it.controlsVisible
            binding.mediaFooter.isVisible = it.mainVisible
            binding.mediaLoading.isVisible = it.loadingVisible
            binding.mediaSeekerTime.isVisible = it.seekerVisible
            binding.mediaScrim.isVisible = it.mainVisible
            binding.mediaSkipContainer.isVisible = it.skipVisible
        }.launchIn(holder.coroutineScope)

        mediaButtonsController.onAnyTap = {
            uiVisbilityController.showMain()
        }

        gestureController.singleTapListener = {
            uiVisbilityController.toggleMainVisible()
        }

        gestureController.doubleTapSeekerState.onEach {
            uiVisbilityController.updateDoubleTapSeeker(it.isActive)
        }.launchIn(holder.coroutineScope)

        gestureController.scrollSeekerState.onEach {
            uiVisbilityController.updateScrollSeeker(it.isActive)
        }.launchIn(holder.coroutineScope)

        timelineController.seekState.onEach {
            uiVisbilityController.updateSlider(it != null)
        }.launchIn(holder.coroutineScope)

        skipsController.currentSkip.onEach {
            uiVisbilityController.updateSkip(it != null)
        }.launchIn(holder.coroutineScope)

        /*skipsController.setSkips(listOf(
            SkipsController.Skip(10000, 100000)
        ))*/

    }

    fun setPlayer(player: Player?) {
        holder.setPlayer(player)
    }


}