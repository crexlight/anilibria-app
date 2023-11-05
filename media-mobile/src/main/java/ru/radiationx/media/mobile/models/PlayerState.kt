package ru.radiationx.media.mobile.models

internal data class PlayerState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val videoSize: VideoSizeState = VideoSizeState(),
) {
    val isBlockingLoading = !isPlaying && isLoading && playbackState == PlaybackState.BUFFERING
}