package ru.radiationx.media.mobile.controllers.gesture

import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.radiationx.media.mobile.PlayerFlow
import java.util.concurrent.TimeUnit
import kotlin.math.pow

internal class ScrollSeeker(
    private val playerFlow: PlayerFlow,
    private val gestureView: View,
) {

    private val _state = MutableStateFlow(SeekerState())
    val state = _state.asStateFlow()

    var applyListener: ((SeekerState) -> Unit)? = null

    fun onScroll(deltaX: Float) {
        val percent = ((deltaX / gestureView.width) * 100).toInt()
        val sign = if (percent < 0) -1 else 1
        val seconds = percent.toDouble().pow(2.0).div(25).times(sign).toLong()
        val newDeltaSeek = TimeUnit.SECONDS.toMillis(seconds)

        _state.update {
            val newInitialSeek = if (it.isActive) {
                it.initialSeek
            } else {
                playerFlow.timelineState.value.position
            }
            it.copy(
                isActive = true,
                initialSeek = newInitialSeek,
                deltaSeek = newDeltaSeek
            )
        }
    }

    fun onTouchEnd() {
        if (_state.value.isActive) {
            applyListener?.invoke(_state.value)
        }
        _state.value = SeekerState()
    }
}