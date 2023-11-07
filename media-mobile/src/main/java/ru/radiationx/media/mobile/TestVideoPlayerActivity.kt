package ru.radiationx.media.mobile

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.media3.exoplayer.ExoPlayer
import by.kirich1409.viewbindingdelegate.viewBinding
import ru.radiationx.media.mobile.databinding.ActivityTestVideoplayerBinding


private class PlayerHolder {
    private var _player: ExoPlayer? = null

    fun init(context: Context) {
        _player = ExoPlayer.Builder(context.applicationContext)
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

    private val binding by viewBinding<ActivityTestVideoplayerBinding>()

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
        binding.playerView.setPlayer(player.getPlayer())


        val uri =
            Uri.parse("https://cache.libria.fun/videos/media/ts/9486/15/480/8a6fb82096b5874a120c6d84b503996a.m3u8")
        binding.playerView.prepare(uri)
        binding.playerView.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.setPlayer(null)
        player.destroy()
    }
}