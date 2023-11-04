package ru.radiationx.media.mobile.controllers

import android.view.TextureView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.AspectRatioFrameLayout
import ru.radiationx.media.mobile.RootPlayerHolder
import ru.radiationx.media.mobile.models.PlaybackState

class OutputController(
    private val holder: RootPlayerHolder,
    private val mediaTextureView: TextureView,
    private val mediaAspectRatio: AspectRatioFrameLayout,
) {

    init {
        holder.flow.playerState
            .filterNot {
                it.videoSize == VideoSize.UNKNOWN || it.playbackState == PlaybackState.IDLE
            }
            .onEach {
                updateAspectRatio()
            }
            .launchIn(holder.coroutineScope)

        holder.addListener(object : RootPlayerHolder.Listener {
            override fun attachPlayer(player: Player) {
                player.setVideoTextureView(mediaTextureView)
            }

            override fun detachPlayer(player: Player) {
                player.clearVideoTextureView(mediaTextureView)
            }
        })
    }

    private fun updateAspectRatio() {
        val videoSize = holder.flow.playerState.value.videoSize
        val width = videoSize.width
        val height = videoSize.height
        val videoAspectRatio = if (height == 0 || width == 0) {
            0f
        } else {
            width * videoSize.pixelWidthHeightRatio / height
        }
        mediaAspectRatio.setAspectRatio(videoAspectRatio)
    }
}