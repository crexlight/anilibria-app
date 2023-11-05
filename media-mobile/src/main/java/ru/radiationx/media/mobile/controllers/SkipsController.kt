package ru.radiationx.media.mobile.controllers

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import ru.radiationx.media.mobile.holder.PlayerAttachListener
import ru.radiationx.media.mobile.PlayerFlow
import ru.radiationx.media.mobile.holder.RootPlayerHolder

internal class SkipsController(
    private val holder: RootPlayerHolder,
    private val coroutineScope: CoroutineScope,
    private val playerFlow: PlayerFlow,
    private val skipButtonCancel: View,
    private val skipButtonSkip: View,
) : PlayerAttachListener {

    private val _skipsData = MutableStateFlow(SkipsData())

    private val _currentSkip = MutableStateFlow<Skip?>(null)
    val currentSkip = _currentSkip.asStateFlow()

    init {
        skipButtonCancel.setOnClickListener {
            cancelCurrentSkip()
        }
        skipButtonSkip.setOnClickListener {
            _currentSkip.value?.also {
                holder.getPlayer()?.seekTo(it.end)
            }
            cancelCurrentSkip()
        }

        combine(
            _skipsData,
            playerFlow.timelineState
        ) { skipsData, timelineState ->
            val skip = skipsData.skips.find {
                checkSkip(it, skipsData.skipped, timelineState.position)
            }
            _currentSkip.value = skip
        }.launchIn(coroutineScope)
    }

    fun setSkips(skips: List<Skip>) {
        _skipsData.value = SkipsData(skips)
    }

    private fun checkSkip(skip: Skip, skipped: Set<Skip>, position: Long): Boolean {
        return skip !in skipped && (position in skip.start..skip.end)
    }

    private fun cancelCurrentSkip() {
        val skip = _currentSkip.value ?: return
        _skipsData.update {
            it.copy(skipped = it.skipped.plus(skip))
        }
    }

    data class Skip(
        val start: Long,
        val end: Long,
    )

    private data class SkipsData(
        val skips: List<Skip> = emptyList(),
        val skipped: Set<Skip> = emptySet(),
    )
}