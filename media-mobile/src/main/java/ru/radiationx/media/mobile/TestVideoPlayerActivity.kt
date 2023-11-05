package ru.radiationx.media.mobile

import android.content.Context
import android.net.Uri
import android.os.Bundle
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
        super.onCreate(savedInstanceState)
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