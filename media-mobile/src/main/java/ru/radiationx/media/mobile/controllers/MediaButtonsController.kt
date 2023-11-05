package ru.radiationx.media.mobile.controllers

import android.widget.Button
import androidx.core.view.isInvisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.PlayerFlow
import ru.radiationx.media.mobile.holder.PlayerAttachListener

internal class MediaButtonsController(
    private val coroutineScope: CoroutineScope,
    private val playerFlow: PlayerFlow,
    private val mediaButtonPrev: Button,
    private val mediaButtonPlay: Button,
    private val mediaButtonNext: Button,
) : PlayerAttachListener {

    var onAnyTap: (() -> Unit)? = null

    init {
        mediaButtonPrev.setOnClickListener {
            onAnyTap?.invoke()
        }
        mediaButtonPlay.setOnClickListener {
            onAnyTap?.invoke()
            if (playerFlow.playerState.value.isPlaying) {
                playerFlow.pause()
            } else {
                playerFlow.play()
            }
        }
        mediaButtonNext.setOnClickListener {
            onAnyTap?.invoke()
        }
        playerFlow.playerState.onEach {
            mediaButtonPlay.text = if (it.playWhenReady && it.isPlaying) "pause" else "play"
            mediaButtonPlay.isInvisible = it.isBlockingLoading
        }.launchIn(coroutineScope)
    }

    fun setHasPrev(state: Boolean) {
        mediaButtonPrev.isInvisible = !state
    }

    fun setHasNext(state: Boolean) {
        mediaButtonNext.isInvisible = !state
    }

}