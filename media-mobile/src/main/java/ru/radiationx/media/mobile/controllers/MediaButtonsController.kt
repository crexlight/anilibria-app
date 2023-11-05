package ru.radiationx.media.mobile.controllers

import android.widget.Button
import androidx.core.view.isInvisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.holder.PlayerAttachListener
import ru.radiationx.media.mobile.PlayerFlow
import ru.radiationx.media.mobile.holder.RootPlayerHolder

internal class MediaButtonsController(
    private val holder: RootPlayerHolder,
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
            val player = holder.getPlayer() ?: return@setOnClickListener
            if (playerFlow.playerState.value.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
        mediaButtonNext.setOnClickListener {
            onAnyTap?.invoke()
        }
        playerFlow.playerState.onEach {
            mediaButtonPlay.text = if (it.isPlaying) "pause" else "play"
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