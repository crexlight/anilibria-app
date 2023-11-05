package ru.radiationx.media.mobile.models

data class TimelineState(
    val duration: Long = 0,
    val position: Long = 0,
    val bufferPercent: Int = 0,
)