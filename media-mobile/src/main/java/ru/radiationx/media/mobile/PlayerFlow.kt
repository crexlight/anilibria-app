package ru.radiationx.media.mobile

import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.media.mobile.models.PlaybackState
import ru.radiationx.media.mobile.models.asPlaybackState

class PlayerFlow(
    private val coroutineScope: CoroutineScope,
) : RootPlayerHolder.Listener {

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            _playerState.update { it.copy(isLoading = isLoading) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            _playerState.update { it.copy(playbackState = playbackState.asPlaybackState()) }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            _playerState.update { it.copy(videoSize = videoSize) }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            updateTimeline(player)
        }
    }

    private var timelineJob: Job? = null

    private val _timelineState = MutableStateFlow(TimelineState())
    val timelineState = _timelineState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    override fun attachPlayer(player: Player) {
        _playerState.update {
            PlayerState(
                isPlaying = player.isPlaying,
                isLoading = player.isLoading,
                playbackState = player.playbackState.asPlaybackState(),
                videoSize = player.videoSize
            )
        }
        updateTimeline(player)
        player.addListener(playerListener)
        timelineJob?.cancel()
        timelineJob = coroutineScope.launch {
            while (true) {
                updateTimeline(player)
                delay(500)
            }
        }
    }

    override fun detachPlayer(player: Player) {
        player.removeListener(playerListener)
        _playerState.value = PlayerState()
        _timelineState.value = TimelineState()
    }

    private fun updateTimeline(player: Player) {
        _timelineState.value = TimelineState(
            player.duration.coerceAtLeast(0),
            player.currentPosition.coerceAtLeast(0),
            player.bufferedPercentage.coerceAtLeast(0)
        )
    }
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val videoSize: VideoSize = VideoSize.UNKNOWN,
) {
    val isBlockingLoading = !isPlaying && isLoading && playbackState == PlaybackState.BUFFERING
}

data class TimelineState(
    val duration: Long = 0,
    val position: Long = 0,
    val bufferPercent: Int = 0,
)
