package ru.radiationx.media.mobile.controllers

import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import ru.radiationx.media.mobile.PlayerFlow
import ru.radiationx.media.mobile.holder.PlayerAttachListener
import ru.radiationx.media.mobile.views.AspectRatioFrameLayout

internal class OutputController(
    private val coroutineScope: CoroutineScope,
    private val playerFlow: PlayerFlow,
    private val mediaTextureView: TextureView,
    private val mediaAspectRatio: AspectRatioFrameLayout,
    private val scaleContainer: FrameLayout,
    private val scaleButton: View,
) : PlayerAttachListener {

    private val _state = MutableStateFlow(ScaleState())


    init {
        playerFlow.playerState
            .map { it.videoSize }
            .distinctUntilChanged()
            .onEach {
                mediaAspectRatio.setAspectRatio(it.aspectRatio)
            }
            .launchIn(coroutineScope)

        scaleContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateScale()
        }
        scaleButton.setOnClickListener {
            _state.update { it.copy(targetFill = !it.targetFill) }
        }

        _state.filter { !it.isLiveScale }.onEach { state ->
            val scale = if (state.canApply && state.targetFill) {
                getScaleByLayout()
            } else {
                1f
            }
            mediaAspectRatio.animate().scaleX(scale).scaleY(scale).start()
            scaleButton.isVisible = state.canApply
        }.launchIn(coroutineScope)
    }

    override fun attachPlayer(player: Player) {
        player.setVideoTextureView(mediaTextureView)
    }

    override fun detachPlayer(player: Player) {
        player.clearVideoTextureView(mediaTextureView)
    }

    fun setLiveScale(scale: Float?) {
        if (scale == null) {
            _state.update { it.copy(isLiveScale = false) }
            return
        }

        val layoutScale = getApplyibleScale()
        val coercedScale = scale.coerceIn(0.95f, layoutScale * 1.05f)
        val layoutScaleDiff = layoutScale - 1f

        _state.update {
            val targetFill = if (it.canApply) {
                coercedScale >= (1f + layoutScaleDiff / 2)
            } else {
                it.targetFill
            }
            it.copy(isLiveScale = true, targetFill = targetFill)
        }
        mediaAspectRatio.scaleX = coercedScale
        mediaAspectRatio.scaleY = coercedScale
    }

    private fun updateScale() {
        val scale = getScaleByLayout()
        val canApply = scale <= 1.5
        _state.update { it.copy(canApply = canApply) }
    }

    private fun getApplyibleScale(): Float {
        val scale = getScaleByLayout()
        return if (scale <= 1.5) scale else 1f
    }

    private fun getScaleByLayout(): Float {
        val videoWidth = mediaAspectRatio.width.toFloat()
        val videoHeight = mediaAspectRatio.height.toFloat()

        val scaleWidth = scaleContainer.width.toFloat()
        val scaleHeight = scaleContainer.height.toFloat()

        val widthPercent = scaleWidth / videoWidth
        val heightPercet = scaleHeight / videoHeight

        return maxOf(widthPercent, heightPercet).coerceAtLeast(1f)
    }

    data class ScaleState(
        val canApply: Boolean = false,
        val targetFill: Boolean = false,
        val isLiveScale: Boolean = false,
    )

    enum class ScaleType {
        FIT,
        FILL
    }
}