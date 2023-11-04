package ru.radiationx.anilibria.ui.activities.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import ru.radiationx.anilibria.R
import ru.radiationx.anilibria.databinding.ActivityVideoplayerBinding
import ru.radiationx.anilibria.ui.activities.BaseActivity
import ru.radiationx.data.entity.domain.types.EpisodeId
import ru.radiationx.data.interactors.ReleaseInteractor
import ru.radiationx.quill.inject
import ru.radiationx.shared.ktx.android.getExtraNotNull

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

class VideoPlayerActivity : BaseActivity(R.layout.activity_videoplayer) {

    companion object {
        private const val ARG_EPISODE_ID = "ARG_EPISODE_ID"

        fun newIntent(context: Context, episodeId: EpisodeId) =
            Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(ARG_EPISODE_ID, episodeId)
            }
    }

    private val releaseInteractor by inject<ReleaseInteractor>()

    private val binding by viewBinding<ActivityVideoplayerBinding>()

    private val player = PlayerHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player.init(this)
        binding.playerView.setPlayer(player.getPlayer())

        val episodeId = getExtraNotNull<EpisodeId>(ARG_EPISODE_ID)
        lifecycleScope.launch {
            val release = releaseInteractor.getFull(episodeId.releaseId)!!
            val episode = release.episodes.find { it.id == episodeId }!!
            val uri = episode.let { it.urlHd ?: it.urlSd }!!.let { Uri.parse(it) }
            player.getPlayer().setMediaItem(MediaItem.fromUri(uri))
            player.getPlayer().prepare()
            player.getPlayer().playWhenReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.setPlayer(null)
        player.destroy()
    }
}