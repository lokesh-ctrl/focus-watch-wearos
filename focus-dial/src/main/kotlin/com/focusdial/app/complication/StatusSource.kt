package com.focusdial.app.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.focusdial.app.FocusSessionManager

class StatusSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("FOCUS").build(),
            contentDescription = PlainComplicationText.Builder("Session status").build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val manager = FocusSessionManager.getInstance(this)

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(manager.statusText).build(),
            contentDescription = PlainComplicationText.Builder("Status: ${manager.statusText}").build()
        ).build()
    }
}
