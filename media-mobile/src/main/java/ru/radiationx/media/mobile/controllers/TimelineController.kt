package ru.radiationx.media.mobile.controllers

import android.util.Log
import android.widget.TextView
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnChangeListener
import com.google.android.material.slider.Slider.OnSliderTouchListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.RootPlayerHolder
import ru.radiationx.media.mobile.TimeFormatter
import ru.radiationx.media.mobile.TimelineState

class TimelineController(
    private val holder: RootPlayerHolder,
    private val slider: Slider,
    private val bufferingSlider: Slider,
    private val mediaTime: TextView,
) {

    private val _seekState = MutableStateFlow<Long?>(null)
    val seekState = _seekState.asStateFlow()

    init {
        slider.valueFrom = 0f
        slider.valueTo = 1f
        slider.labelBehavior = LabelFormatter.LABEL_GONE

        bufferingSlider.valueFrom = 0f
        bufferingSlider.valueTo = 100f
        bufferingSlider.labelBehavior = LabelFormatter.LABEL_GONE

        slider.addOnSliderTouchListener(object : OnSliderTouchListener {

            private val changeListener = OnChangeListener { _, value, fromUser ->
                if (!fromUser) return@OnChangeListener
                _seekState.value = value.toLong()
            }

            override fun onStartTrackingTouch(slider: Slider) {
                Log.d("kekeke", "onStartTrackingTouch")
                _seekState.value = slider.value.toLong()
                slider.addOnChangeListener(changeListener)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                Log.d("kekeke", "onStopTrackingTouch")
                slider.removeOnChangeListener(changeListener)
                _seekState.value = null
                holder.getPlayer()?.seekTo(slider.value.toLong())
            }
        })

        val timelineState = holder.flow.timelineState

        timelineState.map { it.duration }.distinctUntilChanged().onEach {
            val newValue = it.toFloat()
            if (newValue > slider.valueFrom) {
                slider.valueTo = newValue
            }
        }.launchIn(holder.coroutineScope)

        timelineState.map { it.bufferPercent }.distinctUntilChanged().onEach {
            bufferingSlider.value = it.toFloat()
        }.launchIn(holder.coroutineScope)

        combine(timelineState, seekState) { timeline, seek ->
            if (seek == null) {
                slider.value = timeline.position.toFloat()
            }
            mediaTime.text = timeline.formatTime(seek)
        }.launchIn(holder.coroutineScope)
    }

    private fun TimelineState.formatTime(seek: Long?): String {
        return "${TimeFormatter.format(seek ?: position)} / ${TimeFormatter.format(duration)}"
    }
}