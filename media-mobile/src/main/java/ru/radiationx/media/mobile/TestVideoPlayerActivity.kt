package ru.radiationx.media.mobile

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
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
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.net.CronetProviderInstaller
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import org.chromium.net.CronetEngine
import org.chromium.net.CronetProvider
import ru.radiationx.media.mobile.databinding.ActivityTestVideoplayerBinding
import java.util.concurrent.Executors


private class PlayerHolder {
    private var _player: ExoPlayer? = null

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


        val finalFactory = cronetSourceFactory ?: okHttpDataSourceFactory
        val dataSourceFactory = DefaultDataSource.Factory(context, finalFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context).apply {
            setDataSourceFactory(dataSourceFactory)
        }

        _player = ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    fun destroy() {
        _player?.release()
        _player = null
    }

    fun getPlayer(): ExoPlayer {
        return requireNotNull(_player)
    }
}

class TestVideoPlayerActivity : FragmentActivity(R.layout.activity_test_videoplayer) {

    companion object {
        private const val REMOTE_CONTROL_PLAY = 1
        private const val REMOTE_CONTROL_PAUSE = 2
        private const val REMOTE_CONTROL_PREV = 3
        private const val REMOTE_CONTROL_NEXT = 4
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


    private val binding by viewBinding<ActivityTestVideoplayerBinding>()

    private val player = PlayerHolder()

    private val pipController = PictureInPictureController(this)

    private val fullScreenController = FullScreenController(this)

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


        val uri =
            Uri.parse("https://cache.libria.fun/videos/media/ts/9486/15/480/8a6fb82096b5874a120c6d84b503996a.m3u8")
        binding.playerView.prepare(uri)
        //binding.playerView.play()


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

    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.setPlayer(null)
        player.destroy()
    }
}