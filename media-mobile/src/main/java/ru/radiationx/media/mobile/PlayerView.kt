package ru.radiationx.media.mobile

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.media3.common.Player
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.controllers.MediaButtonsController
import ru.radiationx.media.mobile.controllers.OutputController
import ru.radiationx.media.mobile.controllers.TimelineController
import ru.radiationx.media.mobile.controllers.UiVisbilityController
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

    init {
        holder.flow.playerState.onEach {
            Log.d("kekeke", "player state $it")
        }.launchIn(holder.coroutineScope)

        uiVisbilityController.state.onEach {
            binding.mediaControls.isVisible = it.controlsVisible
            binding.mediaFooter.isVisible = it.controlsVisible
        }.launchIn(holder.coroutineScope)

        mediaButtonsController.onAnyTap = {
            uiVisbilityController.showControls()
        }

        timelineController.seekState.onEach {

        }.launchIn(holder.coroutineScope)

        binding.mediaControlsContainer.setOnClickListener {
            uiVisbilityController.onSingleTap()
        }

    }

    fun setPlayer(player: Player?) {
        holder.setPlayer(player)
    }


}