package ru.radiationx.media.mobile

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RootPlayerHolder {

    private var _player: Player? = null
    private val listeners = mutableListOf<Listener>()

    val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val flow = PlayerFlow(coroutineScope)

    init {
        addListener(flow)
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun setPlayer(player: Player?) {
        _player?.also { detachPlayer(it) }
        _player = player
        _player?.also { attachPlayer(it) }
    }

    fun getPlayer(): Player? {
        return _player
    }

    fun requirePlayer(): Player {
        return requireNotNull(_player)
    }

    private fun attachPlayer(player: Player) {
        listeners.forEach { it.attachPlayer(player) }
    }

    private fun detachPlayer(player: Player) {
        listeners.forEach { it.detachPlayer(player) }
    }

    interface Listener {
        fun attachPlayer(player: Player)
        fun detachPlayer(player: Player)
    }
}