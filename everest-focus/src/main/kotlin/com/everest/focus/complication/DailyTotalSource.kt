package com.everest.focus.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.everest.focus.FocusSessionManager
import com.everest.focus.data.FocusPreferences
import com.everest.focus.data.FocusState

class DailyTotalSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("3h 20m").build(),
            contentDescription = PlainComplicationText.Builder("Daily deep work total").build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = FocusPreferences(this)
        var totalMillis = prefs.getDailyTotalMillis()

        val manager = FocusSessionManager.getInstance(this)
        if (manager.state.value is FocusState.Focus) {
            totalMillis += manager.elapsedFocusMillis
        }

        val totalMinutes = (totalMillis / 60_000).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val text = when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("$text deep work today").build()
        ).build()
    }
}
