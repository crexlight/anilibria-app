package ru.radiationx.media.mobile.controllers

import android.widget.TextView
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnChangeListener
import com.google.android.material.slider.Slider.OnSliderTouchListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.radiationx.media.mobile.PlayerFlow
import ru.radiationx.media.mobile.holder.PlayerAttachListener
import ru.radiationx.media.mobile.models.TimelineState
import ru.radiationx.media.mobile.utils.TimeFormatter

internal class TimelineController(
    private val coroutineScope: CoroutineScope,
    private val playerFlow: PlayerFlow,
    private val slider: Slider,
    private val bufferingSlider: Slider,
    private val mediaTime: TextView,
) : PlayerAttachListener {

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
                _seekState.value = slider.value.toLong()
                slider.addOnChangeListener(changeListener)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                slider.removeOnChangeListener(changeListener)
                _seekState.value = null
                playerFlow.seekTo(slider.value.toLong())
            }
        })

        val timelineState = playerFlow.timelineState

        timelineState.map { it.duration }.distinctUntilChanged().onEach {
            val newValue = it.toFloat()
            if (newValue > slider.valueFrom) {
                slider.valueTo = newValue
            }
        }.launchIn(coroutineScope)

        timelineState.onEach {
            bufferingSlider.value = it.bufferPercent.coerceIn(0, 100).toFloat()
        }.launchIn(coroutineScope)

        combine(timelineState, seekState) { timeline, seek ->
            if (seek == null) {
                slider.value = timeline.position.coerceIn(0, timeline.duration).toFloat()
            }
            mediaTime.text = timeline.formatTime(seek)
        }.launchIn(coroutineScope)
    }

    private fun TimelineState.formatTime(seek: Long?): String {
        return "${TimeFormatter.format(seek ?: position)} / ${TimeFormatter.format(duration)}"
    }
}