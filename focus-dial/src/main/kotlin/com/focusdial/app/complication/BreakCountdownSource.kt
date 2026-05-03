package com.focusdial.app.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.focusdial.app.FocusSessionManager
import com.focusdial.app.data.FocusState

class BreakCountdownSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("Break 13m").build(),
            contentDescription = PlainComplicationText.Builder("Break countdown").build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val manager = FocusSessionManager.getInstance(this)

        val text = when (manager.state.value) {
            is FocusState.Focus -> {
                val remaining = manager.remainingFocusMillis
                val minutes = (remaining / 60_000).toInt()
                "Break ${minutes}m"
            }
            is FocusState.Break -> {
                val remaining = manager.remainingBreakMillis
                val minutes = (remaining / 60_000).toInt()
                if (minutes > 0) "Rest ${minutes}m" else "Break!"
            }
            is FocusState.Idle -> "Ready"
        }

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Break info").build()
        ).build()
    }
}
