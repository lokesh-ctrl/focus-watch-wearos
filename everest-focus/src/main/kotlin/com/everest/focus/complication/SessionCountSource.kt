package com.everest.focus.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.everest.focus.data.FocusPreferences

class SessionCountSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("●●●○").build(),
            contentDescription = PlainComplicationText.Builder("Sessions completed").build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = FocusPreferences(this)
        val completed = prefs.getCompletedSessions()
        val goal = prefs.getDailyGoal()

        val dots = buildString {
            repeat(completed.coerceAtMost(goal)) { append("●") }
            repeat((goal - completed).coerceAtLeast(0)) { append("○") }
        }

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(dots).build(),
            contentDescription = PlainComplicationText.Builder("$completed of $goal sessions").build()
        ).build()
    }
}
