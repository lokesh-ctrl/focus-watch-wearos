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

        return when (request.complicationType) {
            ComplicationType.RANGED_VALUE -> {
                val progress = manager.progressFraction
                val elapsed = manager.elapsedFocusMillis
                val minutes = (elapsed / 60_000).toInt()
                val text = if (manager.state.value is FocusState.Focus) "${minutes}min" else "--"

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
                val elapsed = manager.elapsedFocusMillis
                val minutes = (elapsed / 60_000).toInt()
                val text = if (manager.state.value is FocusState.Focus) "${minutes}min" else "--"

                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder("Focus duration").build()
                ).build()
            }
        }
    }
}
