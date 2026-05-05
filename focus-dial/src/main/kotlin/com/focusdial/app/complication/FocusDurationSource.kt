package com.focusdial.app.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.focusdial.app.FocusSessionManager
import com.focusdial.app.data.FocusState

class FocusDurationSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return when (type) {
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 0.6f,
                min = 0f,
                max = 1f,
                contentDescription = PlainComplicationText.Builder("Focus progress").build()
            )
                .setText(PlainComplicationText.Builder("30min").build())
                .build()
            else -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("30min").build(),
                contentDescription = PlainComplicationText.Builder("Focus duration").build()
            ).build()
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val manager = FocusSessionManager.getInstance(this)
        val state = manager.state.value

        val (progress, text) = when (state) {
            is FocusState.Focus -> {
                val p = manager.progressFraction
                val minutes = (manager.elapsedFocusMillis / 60_000).toInt()
                p to "${minutes}min"
            }
            is FocusState.Paused -> {
                val p = (state.elapsedBeforePauseMillis.toFloat() / state.durationMillis).coerceIn(0f, 1f)
                val minutes = (state.elapsedBeforePauseMillis / 60_000).toInt()
                p to "⏸${minutes}m"
            }
            else -> 0f to "--"
        }

        return when (request.complicationType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = progress,
                    min = 0f,
                    max = 1f,
                    contentDescription = PlainComplicationText.Builder("Focus progress").build()
                )
                    .setText(PlainComplicationText.Builder(text).build())
                    .build()
            }
            else -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder("Focus duration").build()
                ).build()
            }
        }
    }
}
