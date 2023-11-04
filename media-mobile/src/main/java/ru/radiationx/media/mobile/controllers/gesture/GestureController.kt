package ru.radiationx.media.mobile.controllers.gesture

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.RootPlayerHolder
import ru.radiationx.media.mobile.TimeFormatter

@SuppressLint("ClickableViewAccessibility")
class GestureController(
    private val holder: RootPlayerHolder,
    private val gestureView: View,
    private val seekerTime: TextView,
) {

    private val gestureListener = GestureListener()
    private val gestureDetector = GestureDetectorCompat(gestureView.context, gestureListener)

    // todo use for youtube scale gestures
    private val scaledetector = ScaleGestureDetector(
        gestureView.context,
        ScaleGestureDetector.SimpleOnScaleGestureListener()
    )

    private val doubleTapSeeker = DoubleTapSeeker(holder, gestureView)
    private val scrollSeeker = ScrollSeeker(holder, gestureView)

    var singleTapListener: (() -> Unit)? = null

    val doubleTapSeekerState = doubleTapSeeker.state
    val scrollSeekerState = scrollSeeker.state

    init {
        gestureListener.scrollAllowProvider = { scaledetector.isInProgress }

        gestureListener.singleTapListener = {
            singleTapListener?.invoke()
        }
        gestureListener.doubleTapListener = {
            doubleTapSeeker.onDoubleTap(it)
        }
        gestureListener.scrollListener = {
            scrollSeeker.onScroll(it)
        }

        gestureView.setOnTouchListener { _, event ->
            var result = scaledetector.onTouchEvent(event)
            result = gestureDetector.onTouchEvent(event) || result
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                scrollSeeker.onTouchEnd()
            }
            result
        }

        doubleTapSeeker.applyListener = {
            holder.getPlayer()?.seekTo(it.initialSeek + it.deltaSeek)
        }

        scrollSeeker.applyListener = {
            holder.getPlayer()?.seekTo(it.initialSeek + it.deltaSeek)
        }

        doubleTapSeeker.state.onEach {
            seekerTime.text = TimeFormatter.format(it.deltaSeek, true)
        }.launchIn(holder.coroutineScope)

        scrollSeeker.state.onEach {
            seekerTime.text = TimeFormatter.format(it.deltaSeek, true)
        }.launchIn(holder.coroutineScope)
    }
}

