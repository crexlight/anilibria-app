package ru.radiationx.media.mobile.controllers

import android.widget.ImageButton
import androidx.core.view.isInvisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.PlayerFlow
import ru.radiationx.media.mobile.R
import ru.radiationx.media.mobile.holder.PlayerAttachListener
import ru.radiationx.shared.ktx.android.setCompatDrawable

internal class MediaButtonsController(
    private val coroutineScope: CoroutineScope,
    private val playerFlow: PlayerFlow,
    private val mediaButtonPrev: ImageButton,
    private val mediaButtonPlay: ImageButton,
    private val mediaButtonNext: ImageButton,
) : PlayerAttachListener {

    var onAnyTap: (() -> Unit)? = null

    init {
        mediaButtonPrev.setOnClickListener {
            onAnyTap?.invoke()
            playerFlow.prev()
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
            playerFlow.next()
        }
        playerFlow.playerState.onEach {
            val icRes = if (it.playWhenReady && it.isPlaying) {
                R.drawable.ic_media_pause_24
            } else {
                R.drawable.ic_media_play_arrow_24
            }
            mediaButtonPlay.setCompatDrawable(icRes)
            mediaButtonPlay.isInvisible = it.isBlockingLoading || !it.commands.playPause
        }.launchIn(coroutineScope)

        playerFlow.playerState.map { it.commands }.distinctUntilChanged().onEach {
            mediaButtonPrev.isInvisible = !it.seekToPreviousMediaItem
            mediaButtonNext.isInvisible = !it.seekToNextMediaItem
        }.launchIn(coroutineScope)
    }

}