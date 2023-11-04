package ru.radiationx.media.mobile.controllers.gesture

import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.media.mobile.RootPlayerHolder
import java.util.concurrent.TimeUnit

@OptIn(FlowPreview::class)
class DoubleTapSeeker(
    private val holder: RootPlayerHolder,
    private val gestureView: View,
) {

    private val doubleTapEvents = MutableSharedFlow<MotionEvent>()

    private val _state = MutableStateFlow(SeekerState())
    val state = _state.asStateFlow()

    var applyListener: ((SeekerState) -> Unit)? = null

    init {
        doubleTapEvents
            .filter { event ->
                val width = gestureView.width
                val allowWidth = width / 3
                val seconds = when (event.x.toInt()) {
                    in 0..allowWidth -> -10L
                    in width - allowWidth..width -> 10L
                    else -> return@filter false
                }
                val delta = TimeUnit.SECONDS.toMillis(seconds)
                _state.update {
                    val newInitialSeek = if (it.isActive) {
                        it.initialSeek
                    } else {
                        holder.getPlayer()?.currentPosition ?: 0
                    }
                    it.copy(
                        isActive = true,
                        initialSeek = newInitialSeek,
                        deltaSeek = it.deltaSeek + delta
                    )
                }
                true
            }
            .debounce(500L)
            .onEach {
                if (_state.value.isActive) {
                    applyListener?.invoke(_state.value)
                }
                _state.value = SeekerState()
            }
            .launchIn(holder.coroutineScope)
    }

    fun onDoubleTap(event: MotionEvent) {
        holder.coroutineScope.launch { doubleTapEvents.emit(event) }
    }
}