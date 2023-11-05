package ru.radiationx.media.mobile.controllers

import android.view.TextureView
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.views.AspectRatioFrameLayout
import ru.radiationx.media.mobile.PlayerFlow
import ru.radiationx.media.mobile.holder.PlayerAttachListener

internal class OutputController(
    private val coroutineScope: CoroutineScope,
    private val playerFlow: PlayerFlow,
    private val mediaTextureView: TextureView,
    private val mediaAspectRatio: AspectRatioFrameLayout,
) : PlayerAttachListener {

    init {
        playerFlow.playerState
            .map { it.videoSize }
            .distinctUntilChanged()
            .onEach {
                mediaAspectRatio.setAspectRatio(it.aspectRatio)
            }
            .launchIn(coroutineScope)
    }

    override fun attachPlayer(player: Player) {
        player.setVideoTextureView(mediaTextureView)
    }

    override fun detachPlayer(player: Player) {
        player.clearVideoTextureView(mediaTextureView)
    }
}