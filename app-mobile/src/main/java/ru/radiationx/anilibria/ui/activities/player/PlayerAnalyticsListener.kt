package ru.radiationx.anilibria.ui.activities.player

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import ru.radiationx.anilibria.ui.activities.player.di.SharedPlayerData
import ru.radiationx.data.SharedBuildConfig
import ru.radiationx.data.analytics.features.PlayerAnalytics
import ru.radiationx.data.analytics.features.mapper.toAnalyticsQuality
import ru.radiationx.data.datasource.holders.PreferencesHolder
import ru.radiationx.data.entity.domain.types.EpisodeId
import timber.log.Timber
import java.io.IOException
import java.util.LinkedList
import javax.inject.Inject


class PlayerAnalyticsListener @Inject constructor(
    private val playerAnalytics: PlayerAnalytics,
    private val buildConfig: SharedBuildConfig,
    private val sharedPlayerData: SharedPlayerData,
    private val preferences: PreferencesHolder,
) : AnalyticsListener {

    private val times = LinkedList<Long>()
    private val hostCounter = mutableMapOf<String, Int>()

    private val episodeId: EpisodeId
        get() = sharedPlayerData.episodeId.value

    private var latestLoadData: LoadData? = null

    @UnstableApi
    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean,
    ) {
        latestLoadData = loadEventInfo.toLoadData()
        Timber.e(error, "onLoadError")
    }

    @UnstableApi
    override fun onPlayerError(
        eventTime: AnalyticsListener.EventTime,
        error: PlaybackException,
    ) {
        val info = "${error.errorCode}, ${error.errorCodeName}"
        playerAnalytics.playerError(error, createErrorData(eventTime, info))
    }

    @UnstableApi
    override fun onAudioCodecError(
        eventTime: AnalyticsListener.EventTime,
        audioCodecError: Exception,
    ) {
        playerAnalytics.playerAudioCodecError(audioCodecError, createErrorData(eventTime, null))
    }

    @UnstableApi
    override fun onAudioSinkError(
        eventTime: AnalyticsListener.EventTime,
        audioSinkError: Exception,
    ) {
        playerAnalytics.playerAudioSinkError(audioSinkError, createErrorData(eventTime, null))
    }

    @UnstableApi
    override fun onVideoCodecError(
        eventTime: AnalyticsListener.EventTime,
        videoCodecError: Exception,
    ) {
        playerAnalytics.playerVideoCodecError(videoCodecError, createErrorData(eventTime, null))
    }

    @UnstableApi
    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
    ) {
        if (!buildConfig.debug) {
            return
        }
        times.add(loadEventInfo.loadDurationMs)
        val protocol = loadEventInfo.responseHeaders.let {
            (it["x-server-proto"] ?: it["X-Server-Proto"])?.firstOrNull()
        }.orEmpty()
        val frontHost = loadEventInfo.responseHeaders.let {
            (it["front-hostname"] ?: it["Front-Hostname"])?.firstOrNull()
        }.orEmpty()
        hostCounter[frontHost] = hostCounter.getOrPut(frontHost) { 0 } + 1


        val durationMs = loadEventInfo.loadDurationMs
        val average = times.average()
        val hostCount = hostCounter[frontHost]

        Timber
            .tag("onLoadCompleted")
            .d("loadEventInfo.loadDurationMs $durationMs, average $average, protocol $protocol, hostname $frontHost[$hostCount]")
    }

    @UnstableApi
    private fun createErrorData(
        eventTime: AnalyticsListener.EventTime,
        info: String?,
    ): PlayerAnalytics.ErrorData = PlayerAnalytics.ErrorData(
        episodeId = episodeId,
        position = eventTime.currentPlaybackPositionMs,
        quality = preferences.playerQuality.value.toAnalyticsQuality(),
        info = info,
        latestLoadUri = latestLoadData?.uri,
        headerProtocol = latestLoadData?.protocol,
        headerHost = latestLoadData?.hostName
    )

    @UnstableApi
    private fun LoadEventInfo.toLoadData() = LoadData(
        uri = uri,
        protocol = responseHeaders.let {
            (it["x-server-proto"] ?: it["X-Server-Proto"])?.firstOrNull()
        },
        hostName = responseHeaders.let {
            (it["front-hostname"] ?: it["Front-Hostname"])?.firstOrNull()
        }
    )

    private data class LoadData(
        val uri: Uri,
        val protocol: String?,
        val hostName: String?,
    )
}