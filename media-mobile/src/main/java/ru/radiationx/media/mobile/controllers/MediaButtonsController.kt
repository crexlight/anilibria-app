package ru.radiationx.media.mobile.controllers

import android.widget.Button
import androidx.core.view.isInvisible
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.RootPlayerHolder

class MediaButtonsController(
    private val holder: RootPlayerHolder,
    private val mediaButtonPrev: Button,
    private val mediaButtonPlay: Button,
    private val mediaButtonNext: Button,
) {

    var onAnyTap: (() -> Unit)? = null

    init {
        mediaButtonPrev.setOnClickListener {
            onAnyTap?.invoke()
        }
        mediaButtonPlay.setOnClickListener {
            onAnyTap?.invoke()
            val player = holder.getPlayer() ?: return@setOnClickListener
            if (holder.flow.playerState.value.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
        mediaButtonNext.setOnClickListener {
            onAnyTap?.invoke()
        }
        holder.flow.playerState.map { it.isPlaying }.onEach {
            mediaButtonPlay.text = if (it) "pause" else "play"
        }.launchIn(holder.coroutineScope)
    }

    fun setHasPrev(state: Boolean) {
        mediaButtonPrev.isInvisible = !state
    }

    fun setHasNext(state: Boolean) {
        mediaButtonNext.isInvisible = !state
    }

}