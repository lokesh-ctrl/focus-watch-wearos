package com.focusdial.app.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class CalendarHelper(private val context: Context) {

    data class UpcomingEvent(
        val title: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long
    )

    fun getNextEvent(): UpcomingEvent? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val now = System.currentTimeMillis()
        val twoHoursLater = now + 2 * 60 * 60 * 1000L

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
        )

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString())
            .appendPath(twoHoursLater.toString())
            .build()

        context.contentResolver.query(
            uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val title = cursor.getString(0) ?: "Event"
                val start = cursor.getLong(1)
                val end = cursor.getLong(2)
                if (start > now) {
                    return UpcomingEvent(title, start, end)
                }
            }
        }
        return null
    }

    fun formatEventHint(event: UpcomingEvent?): String? {
        if (event == null) return null
        val minutesUntil = ((event.startTimeMillis - System.currentTimeMillis()) / 60_000).toInt()
        return when {
            minutesUntil <= 0 -> null
            minutesUntil < 60 -> "Meeting in ${minutesUntil}min"
            else -> {
                val hour = LocalTime.ofInstant(
                    Instant.ofEpochMilli(event.startTimeMillis), ZoneId.systemDefault()
                ).hour
                val formatted = if (hour > 12) "${hour - 12}pm" else "${hour}am"
                "Free until $formatted"
            }
        }
    }

    fun shouldSuggestShorterFocus(event: UpcomingEvent?, configuredFocusMinutes: Int): Int? {
        if (event == null) return null
        val minutesUntil = ((event.startTimeMillis - System.currentTimeMillis()) / 60_000).toInt()
        if (minutesUntil in 15 until configuredFocusMinutes) {
            return minutesUntil - 5
        }
        return null
    }
}
