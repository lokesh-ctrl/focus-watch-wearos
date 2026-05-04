package com.focusdial.app

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import com.focusdial.app.data.HistoryRepository
import com.focusdial.app.data.db.DaySummaryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class InsightsActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val historyRepo = HistoryRepository(this)

        scope.launch {
            val summaries = historyRepo.getWeeklySummaries()
            val weeklyTotal = historyRepo.getWeeklyTotalMillis()
            val previousWeekTotal = historyRepo.getPreviousWeekTotalMillis()
            val streak = historyRepo.getCurrentStreak()
            val avgScore = historyRepo.getDailyAverageScore()

            val chartView = InsightsChartView(
                context = this@InsightsActivity,
                summaries = summaries,
                weeklyTotalMillis = weeklyTotal,
                previousWeekTotalMillis = previousWeekTotal,
                streak = streak,
                averageScore = avgScore
            )

            val scrollView = ScrollView(this@InsightsActivity).apply {
                setBackgroundColor(Color.BLACK)
                addView(chartView)
            }
            setContentView(scrollView)
        }
    }
}

private class InsightsChartView(
    context: android.content.Context,
    private val summaries: List<DaySummaryEntity>,
    private val weeklyTotalMillis: Long,
    private val previousWeekTotalMillis: Long,
    private val streak: Int,
    private val averageScore: Int
) : View(context) {

    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val labelPaint = Paint().apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 13f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val valuePaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        textSize = 18f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val barPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val barBgPaint = Paint().apply {
        color = Color.parseColor("#222222")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bestBarPaint = Paint().apply {
        color = Color.parseColor("#81C784")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dayLabelPaint = Paint().apply {
        color = Color.parseColor("#888888")
        textSize = 11f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val trendPaint = Paint().apply {
        color = Color.WHITE
        textSize = 16f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        minimumHeight = 520
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, 520)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f

        // Title
        canvas.drawText("Weekly Insights", cx, 35f, titlePaint)

        // Weekly total
        val hours = (weeklyTotalMillis / 3_600_000).toInt()
        val minutes = ((weeklyTotalMillis % 3_600_000) / 60_000).toInt()
        val totalText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        canvas.drawText(totalText, cx, 70f, valuePaint)
        canvas.drawText("total deep work", cx, 88f, labelPaint)

        // Trend arrow
        val trend = when {
            previousWeekTotalMillis == 0L -> "—"
            weeklyTotalMillis > previousWeekTotalMillis -> "↑"
            weeklyTotalMillis < previousWeekTotalMillis -> "↓"
            else -> "→"
        }
        val trendColor = when (trend) {
            "↑" -> Color.parseColor("#4CAF50")
            "↓" -> Color.parseColor("#FF5722")
            else -> Color.parseColor("#888888")
        }
        trendPaint.color = trendColor
        canvas.drawText("$trend vs last week", cx, 115f, trendPaint)

        // Bar chart (7 days)
        drawBarChart(canvas)

        // Stats row
        val statsY = 380f
        canvas.drawText("${streak}d", cx - 70f, statsY, valuePaint)
        canvas.drawText("streak", cx - 70f, statsY + 18f, labelPaint)

        val scoreText = if (averageScore > 0) "$averageScore" else "—"
        canvas.drawText(scoreText, cx, statsY, valuePaint)
        canvas.drawText("avg score", cx, statsY + 18f, labelPaint)

        val sessionsThisWeek = summaries.sumOf { it.sessionsCompleted }
        canvas.drawText("$sessionsThisWeek", cx + 70f, statsY, valuePaint)
        canvas.drawText("sessions", cx + 70f, statsY + 18f, labelPaint)

        // Best day
        val bestDay = summaries.maxByOrNull { it.totalFocusMillis }
        if (bestDay != null && bestDay.totalFocusMillis > 0) {
            val bestDate = LocalDate.parse(bestDay.dateKey)
            val dayName = bestDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val bestMin = (bestDay.totalFocusMillis / 60_000).toInt()
            canvas.drawText("Best: $dayName (${bestMin}m)", cx, 450f, labelPaint)
        }
    }

    private fun drawBarChart(canvas: Canvas) {
        val chartTop = 140f
        val chartBottom = 340f
        val chartHeight = chartBottom - chartTop
        val barWidth = 24f
        val gap = 12f
        val totalBarWidth = 7 * barWidth + 6 * gap
        val startX = (width - totalBarWidth) / 2f

        // Build a map of dateKey -> millis for the past 7 days
        val dataMap = summaries.associateBy { it.dateKey }
        val today = LocalDate.now()
        val days = (0..6).map { today.minusDays((6 - it).toLong()) }

        val maxMillis = summaries.maxOfOrNull { it.totalFocusMillis } ?: 1L
        val bestDateKey = summaries.maxByOrNull { it.totalFocusMillis }?.dateKey

        days.forEachIndexed { index, date ->
            val dateKey = date.toString()
            val millis = dataMap[dateKey]?.totalFocusMillis ?: 0L
            val barHeightFraction = if (maxMillis > 0) millis.toFloat() / maxMillis else 0f

            val x = startX + index * (barWidth + gap)

            // Background bar
            val bgRect = RectF(x, chartTop, x + barWidth, chartBottom)
            canvas.drawRoundRect(bgRect, 4f, 4f, barBgPaint)

            // Data bar
            if (barHeightFraction > 0f) {
                val barTop = chartBottom - (chartHeight * barHeightFraction)
                val dataRect = RectF(x, barTop, x + barWidth, chartBottom)
                val paint = if (dateKey == bestDateKey) bestBarPaint else barPaint
                canvas.drawRoundRect(dataRect, 4f, 4f, paint)
            }

            // Day label
            val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            canvas.drawText(dayLabel, x + barWidth / 2f, chartBottom + 16f, dayLabelPaint)
        }
    }
}
