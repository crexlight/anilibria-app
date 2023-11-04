package ru.radiationx.media.mobile

import java.util.concurrent.TimeUnit

object TimeFormatter {
    private val oneHour = TimeUnit.HOURS.toMillis(1)
    private val oneMinute = TimeUnit.MINUTES.toMillis(1)

    fun format(time: Long): String {
        var temp = time
        val hours = TimeUnit.MILLISECONDS.toHours(temp)
        temp -= hours * oneHour
        val minutes = TimeUnit.MILLISECONDS.toMinutes(temp)
        temp -= minutes * oneMinute
        val seconds = TimeUnit.MILLISECONDS.toSeconds(temp)
        return buildString {
            if (hours > 0) {
                append(hours)
                append(':')
            }
            append(minutes)
            append(':')
            if (seconds < 10) {
                append(0)
            }
            append(seconds)
        }
    }
}