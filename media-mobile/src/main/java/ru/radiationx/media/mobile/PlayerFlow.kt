package ru.radiationx.media.mobile

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.media.mobile.holder.PlayerAttachListener
import ru.radiationx.media.mobile.models.MediaItemTransition
import ru.radiationx.media.mobile.models.PlaybackState
import ru.radiationx.media.mobile.models.PlayerState
import ru.radiationx.media.mobile.models.PlaylistItem
import ru.radiationx.media.mobile.models.PlaylistState
import ru.radiationx.media.mobile.models.TimelineState
import ru.radiationx.media.mobile.models.asPlaybackState
import ru.radiationx.media.mobile.models.asTransitionReason
import ru.radiationx.media.mobile.models.toState

class PlayerFlow(
    private val coroutineScope: CoroutineScope,
) : PlayerAttachListener {

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            _playlistState.update { it.copy(currentItem = it.findByMediaItem(mediaItem)) }
            coroutineScope.launch {
                val transition = MediaItemTransition(mediaItem, reason.asTransitionReason())
                _mediaItemTransitionFlow.emit(transition)
            }
        }

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            super.onAvailableCommandsChanged(availableCommands)
            _playerState.update { it.copy(commands = availableCommands.toState()) }
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            super.onPlayerErrorChanged(error)
            val errorMessage = error?.let {
                "${it.errorCode}, ${it.errorCodeName}"
            }
            _playerState.update { it.copy(errorMessage = errorMessage) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            _playerState.update { it.copy(playWhenReady = playWhenReady) }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            _playerState.update { it.copy(isLoading = isLoading) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            val newState = playbackState.asPlaybackState()
            _playerState.update { it.copy(playbackState = playbackState.asPlaybackState()) }

            // clear completed after state changed
            if (completedNotified && newState != PlaybackState.ENDED) {
                completedNotified = false
            }

            // notify prepared to play
            if (newState == PlaybackState.READY && !prepareNotified) {
                prepareNotified = true
                coroutineScope.launch { _preparedFlow.emit(Unit) }
            }

            // notify play completed
            if (newState == PlaybackState.ENDED && !completedNotified) {
                completedNotified = true
                coroutineScope.launch { _completedFlow.emit(Unit) }
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            if (videoSize == VideoSize.UNKNOWN) {
                return
            }
            _playerState.update { it.copy(videoSize = videoSize.toState()) }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            updateTimeline(player)
        }

    }

    private var _player: Player? = null
    private var prepareNotified = false
    private var completedNotified = false

    private var timelineJob: Job? = null

    private val _playlistState = MutableStateFlow(PlaylistState())
    val playlistState = _playlistState.asStateFlow()

    private val _timelineState = MutableStateFlow(TimelineState())
    val timelineState = _timelineState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    private val _preparedFlow = MutableSharedFlow<Unit>()
    val preparedFlow = _preparedFlow.asSharedFlow()

    private val _completedFlow = MutableSharedFlow<Unit>()
    val completedFlow = _completedFlow.asSharedFlow()

    private val _mediaItemTransitionFlow = MutableSharedFlow<MediaItemTransition>()
    val mediaItemTransitionFlow = _mediaItemTransitionFlow.asSharedFlow()

    override fun attachPlayer(player: Player) {
        _player = player
        _playerState.update {
            PlayerState(
                playWhenReady = player.playWhenReady,
                isPlaying = player.isPlaying,
                isLoading = player.isLoading,
                playbackState = player.playbackState.asPlaybackState(),
                videoSize = player.videoSize.toState(),
                commands = player.availableCommands.toState()
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
        _player = null
        player.removeListener(playerListener)
        _playerState.value = PlayerState()
        _timelineState.value = TimelineState()
    }

    fun prepare(
        playlist: List<PlaylistItem>,
        startIndex: Int? = null,
        startPosition: Long? = null,
    ) {
        prepareNotified = false
        completedNotified = false
        withPlayer { player ->
            val currentItem = startIndex?.let { playlist[it] }
            _playlistState.update { PlaylistState(items = playlist, currentItem = currentItem) }
            player.setMediaItems(
                playlist.map { it.mediaItem },
                startIndex ?: C.INDEX_UNSET,
                startPosition ?: C.TIME_UNSET
            )
            player.prepare()
        }
    }

    fun play() {
        completedNotified = false
        withPlayer {
            it.play()
        }
    }

    fun pause() {
        withPlayer {
            it.pause()
        }
    }

    fun prev() {
        withPlayer {
            it.seekToPreviousMediaItem()
        }
    }

    fun next() {
        withPlayer {
            it.seekToNextMediaItem()
        }
    }

    fun seekTo(position: Long) {
        withPlayer {
            it.seekTo(position)
        }
    }

    fun setSpeed(speed: Float) {
        withPlayer {
            it.setPlaybackSpeed(speed)
        }
    }

    private fun withPlayer(block: (Player) -> Unit) {
        val player = _player ?: return
        if (player.playerError != null) {
            player.prepare()
        }
        block.invoke(player)
    }

    private fun updateTimeline(player: Player) {
        _timelineState.value = TimelineState(
            duration = player.duration.coerceAtLeast(0),
            position = player.currentPosition.coerceAtLeast(0),
            bufferPosition = player.bufferedPosition.coerceAtLeast(0)
        )
    }
}

