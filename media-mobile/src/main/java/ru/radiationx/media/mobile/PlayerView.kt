package ru.radiationx.media.mobile

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.media3.common.Player
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.controllers.ErrorController
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
        mediaAspectRatio = binding.mediaAspectRatio,
        scaleContainer = binding.mediaScaleContainer,
        scaleButton = binding.mediaActionScale
    )

    private val uiVisbilityController = UiVisbilityController(
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        binding = binding
    )

    private val mediaButtonsController = MediaButtonsController(
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        mediaButtonPrev = binding.mediaButtonPrev,
        mediaButtonPlay = binding.mediaButtonPlay,
        mediaButtonNext = binding.mediaButtonNext
    )

    private val timelineController = TimelineController(
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        mediaSeekBar = binding.mediaSeekBar,
        mediaTime = binding.mediaTime
    )

    private val gestureController = GestureController(
        playerFlow = playerFlow,
        coroutineScope = coroutineScope,
        gestureView = binding.mediaGestures,
        seekerTime = binding.mediaSeekerTime,
        mediaAspectRatio = binding.mediaAspectRatio
    )

    private val skipsController = SkipsController(
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        skipButtonCancel = binding.mediaSkipButtonCancel,
        skipButtonSkip = binding.mediaSkipButtonSkip
    )
    private val errorController = ErrorController(
        coroutineScope = coroutineScope,
        playerFlow = playerFlow,
        errorMessageText = binding.mediaErrorMessage,
        errorButtonAction = binding.mediaErrorAction
    )

    init {
        holder.addListener(playerFlow)
        holder.addListener(outputController)
        holder.addListener(uiVisbilityController)
        holder.addListener(mediaButtonsController)
        holder.addListener(timelineController)
        holder.addListener(gestureController)
        holder.addListener(skipsController)
        holder.addListener(errorController)

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

        gestureController.liveScale.onEach {
            outputController.setLiveScale(it)
            uiVisbilityController.updateLiveScale(it != null)
        }.launchIn(coroutineScope)

        timelineController.seekState.onEach {
            uiVisbilityController.updateSlider(it != null)
        }.launchIn(coroutineScope)

        skipsController.currentSkip.onEach {
            uiVisbilityController.updateSkip(it != null)
        }.launchIn(coroutineScope)

        playerFlow.preparedFlow.onEach {
            Log.d("kekeke", "preparedFlow $it")
        }.launchIn(coroutineScope)

        playerFlow.completedFlow.onEach {
            Log.d("kekeke", "completedFlow $it")
        }.launchIn(coroutineScope)

        playerFlow.playerState.onEach {
            Log.d("kekeke", "playerState $it")
        }.launchIn(coroutineScope)

        skipsController.setSkips(
            listOf(
                SkipsController.Skip(2000, 100000)
            )
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barInsets = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val gesturesInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures())

            val footerInsets = Insets.max(barInsets, cutoutInsets)
            binding.mediaFooterContainer.updatePadding(
                left = footerInsets.left,
                top = footerInsets.top,
                right = footerInsets.right,
                bottom = footerInsets.bottom
            )

            binding.mediaGestures.updateLayoutParams<LayoutParams> {
                updateMargins(
                    left = gesturesInsets.left,
                    top = gesturesInsets.top,
                    right = gesturesInsets.right,
                    bottom = gesturesInsets.bottom
                )
            }

            insets
        }
    }

    fun setPlayer(player: Player?) {
        holder.setPlayer(player)
    }

    fun prepare(uri: Uri) {
        playerFlow.prepare(uri)
    }

    fun play() {
        playerFlow.play()
    }

    fun pause() {
        playerFlow.pause()
    }

    fun setSpeed(speed: Float) {
        playerFlow.setSpeed(speed)
    }
}