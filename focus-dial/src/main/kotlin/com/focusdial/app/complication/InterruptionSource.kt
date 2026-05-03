package com.focusdial.app.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.focusdial.app.FocusSessionManager
import com.focusdial.app.data.FocusState

class InterruptionSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("0 ints").build(),
            contentDescription = PlainComplicationText.Builder("Interruptions").build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val manager = FocusSessionManager.getInstance(this)

        val count = when (val state = manager.state.value) {
            is FocusState.Focus -> state.interruptionCount
            else -> 0
        }

        val text = "$count ints"

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("$count interruptions").build()
        ).build()
    }
}
