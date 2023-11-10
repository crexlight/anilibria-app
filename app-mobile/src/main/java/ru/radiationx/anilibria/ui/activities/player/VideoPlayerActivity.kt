package ru.radiationx.anilibria.ui.activities.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.datasource.cronet.CronetUtil
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.session.MediaSession
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.net.CronetProviderInstaller
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.chromium.net.CronetEngine
import org.chromium.net.CronetProvider
import ru.radiationx.anilibria.R
import ru.radiationx.anilibria.databinding.ActivityVideoplayerBinding
import ru.radiationx.anilibria.ui.activities.BaseActivity
import ru.radiationx.data.entity.domain.release.Episode
import ru.radiationx.data.entity.domain.types.EpisodeId
import ru.radiationx.data.interactors.ReleaseInteractor
import ru.radiationx.media.mobile.models.PlaylistItem
import ru.radiationx.media.mobile.models.TimelineSkip
import ru.radiationx.quill.inject
import ru.radiationx.shared.ktx.android.getExtraNotNull
import java.util.concurrent.Executors


private class PlayerHolder {
    private var _player: ExoPlayer? = null
    private var _mediaSession: MediaSession? = null

    fun init(context: Context) {
        CronetProviderInstaller.installProvider(context).addOnCompleteListener {
            Log.e("lololo", "cronet success")
        }
        CronetProvider.getAllProviders(context).forEach {
            Log.e("lololo", "cronet provider $it")
        }
        val okHttpClient = OkHttpClient.Builder().build()
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient).apply {
        }

        /*val provider = CronetProvider.getAllProviders(context).first() {
            it.name == "App-Packaged-Cronet-Provider"
        }*/
        val engine = CronetEngine.Builder(context)
            .enableQuic(false)
            .enableHttp2(true)
            .enableBrotli(false)
            .addQuicHint("cache.libria.fun", 443, 443)
            .apply {
                repeat(30) {
                    addQuicHint("cache-cloud$it.libria.fun", 443, 443)
                }
            }


            .build()
        val engine1 = CronetUtil.buildCronetEngine(context)
        val cronetSourceFactory = engine?.let {
            CronetDataSource.Factory(it, Executors.newFixedThreadPool(4)).apply {
            }
        }


        val finalFactory = /*cronetSourceFactory ?:*/ okHttpDataSourceFactory
        val dataSourceFactory = DefaultDataSource.Factory(context, finalFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context).apply {
            setDataSourceFactory(dataSourceFactory)
        }

        val player = ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
        val mediaSession = MediaSession.Builder(context, player).build()

        _mediaSession = mediaSession
        _player = player
    }

    fun destroy() {
        _mediaSession?.release()
        _mediaSession = null
        _player?.release()
        _player = null
    }

    fun getPlayer(): ExoPlayer {
        return requireNotNull(_player)
    }
}

class VideoPlayerActivity : BaseActivity(R.layout.activity_videoplayer) {

    companion object {
        private const val ARG_EPISODE_ID = "ARG_EPISODE_ID"

        fun newIntent(context: Context, episodeId: EpisodeId) =
            Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(ARG_EPISODE_ID, episodeId)
            }
    }


    private val playAction = PictureInPictureController.Action(
        code = 1,
        title = "Пуск",
        icRes = R.drawable.ic_media_play_arrow_24
    )

    private val pauseAction = PictureInPictureController.Action(
        code = 2,
        title = "Пауза",
        icRes = R.drawable.ic_media_pause_24
    )

    private val pipController = PictureInPictureController(this)

    private val fullScreenController = FullScreenController(this)

    private val releaseInteractor by inject<ReleaseInteractor>()

    private val binding by viewBinding<ActivityVideoplayerBinding>()

    private val player = PlayerHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, binding.root).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.displayCutout())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = if (isInMultiWindowMode) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        player.init(this)
        pipController.init()
        fullScreenController.init()
        binding.playerView.setPlayer(player.getPlayer())
        player.getPlayer()?.addAnalyticsListener(object : AnalyticsListener {
            val times = mutableListOf<Long>()
            val hostCounter = mutableMapOf<String, Int>()

            @UnstableApi
            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
            ) {
                /*loadEventInfo.dataSpec.also {
                    Log.e(
                        "kekeke",
                        "onLoadCompleted kek ${it.httpMethodString}, ${it.uri}, ${it.customData}, ${it.flags}"
                    )
                    Log.e("kekeke", "onLoadCompleted headers ${it.httpRequestHeaders}")

                }*/
                times.add(loadEventInfo.loadDurationMs)
                val protocol = loadEventInfo.responseHeaders.let {
                    (it.get("x-server-proto") ?: it.get("X-Server-Proto"))?.firstOrNull()
                }.orEmpty()
                val frontHost = loadEventInfo.responseHeaders.let {
                    (it.get("front-hostname") ?: it.get("Front-Hostname"))?.firstOrNull()
                }.orEmpty()
                hostCounter[frontHost] = hostCounter.getOrPut(frontHost) { 0 } + 1
                Log.e(
                    "lololo",
                    "loadEventInfo.loadDurationMs ${loadEventInfo.loadDurationMs}, average ${
                        times.average().toLong()
                    }, protocol ${protocol}, hostname $frontHost[${hostCounter.get(frontHost)}]"
                )
                /*loadEventInfo.responseHeaders.also {
                    Log.e(
                        "kekeke",
                        "onLoadCompleted response headers ${it}"
                    )
                }*/
                super.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData)
            }
        })



        binding.playerView.outputState.onEach { videoOutputState ->
            val aspectRatio = videoOutputState.videoSize.let {
                Rational(it.width, it.height)
            }

            //Log.e("kekeke", "outputState ${aspectRatio.toFloat()}, ${videoOutputState.videoSize.aspectRatio}, $videoOutputState")
            pipController.updateParams {
                it.copy(
                    sourceHintRect = videoOutputState.hintRect,
                    aspectRatio = aspectRatio
                )
            }
        }.launchIn(lifecycleScope)

        binding.playerView.playerState.onEach { playerState ->
            val actions = buildList {
                if (playerState.playWhenReady && playerState.isPlaying) {
                    add(pauseAction)
                } else {
                    add(playAction)
                }
            }
            pipController.updateParams {
                it.copy(actions = actions)
            }
        }.launchIn(lifecycleScope)

        pipController.state.onEach {
            binding.playerView.setPipVisible(it.canEnter)
            binding.playerView.setPipActive(it.active)
        }.launchIn(lifecycleScope)

        binding.playerView.onPipClick = {
            pipController.enter()
        }


        pipController.actionsListener = {
            when (it) {
                playAction -> binding.playerView.play()
                pauseAction -> binding.playerView.pause()
            }
        }

        fullScreenController.setFullscreen(true)
        fullScreenController.state.onEach {
            binding.playerView.setFullscreenVisible(it.available)
            binding.playerView.setFullscreenActive(it.actualFullScreen)
        }.launchIn(lifecycleScope)

        binding.playerView.onFullscreenClick = {
            fullScreenController.toggleFullscreen()
        }

        val episodeId = getExtraNotNull<EpisodeId>(ARG_EPISODE_ID)
        lifecycleScope.launch {
            val release = releaseInteractor.getFull(episodeId.releaseId)!!
            binding.playerToolbarTitle.text = release.title
            //binding.playerView.prepare(uri, skips)
            val episodes = release.episodes.asReversed()
            val items = episodes.map { episode ->
                val url = (episode.urlHd ?: episode.urlSd)!!
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(url))
                    .setTag(episode)
                    .build()
                val skips = listOfNotNull(episode.skips?.opening, episode.skips?.ending).map {
                    TimelineSkip(it.start, it.end)
                }
                PlaylistItem(mediaItem, skips)
            }
            val index = episodes.indexOfFirst { it.id == episodeId }
            binding.playerView.prepare(items, index)
        }

        binding.playerView.playlistState.onEach {
            val episode = (it.currentItem?.mediaItem?.localConfiguration?.tag as? Episode?)
            binding.playerToolbarSubtitle.text = episode?.title
        }.launchIn(lifecycleScope)


        binding.playerToolbarBack.setOnClickListener {
            finish()
        }
        val transition = AutoTransition().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            duration = 200
            addTarget(binding.playerToolbar)
        }
        binding.playerView.uiShowState.onEach {
            TransitionManager.beginDelayedTransition(binding.root, transition)
            binding.playerToolbar.isVisible = it
        }.launchIn(lifecycleScope)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val barInsets = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())

            val toolbarInsets = Insets.max(barInsets, cutoutInsets)
            binding.playerToolbar.updatePadding(
                left = toolbarInsets.left,
                top = toolbarInsets.top,
                right = toolbarInsets.right,
                bottom = toolbarInsets.bottom
            )
            binding.playerToolbarTitleContainer.updatePadding(
                left = toolbarInsets.right,
                right = toolbarInsets.left
            )
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.setPlayer(null)
        player.destroy()
    }
}