package com.focusdial.app

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.focusdial.app.calendar.CalendarHelper
import com.focusdial.app.data.FocusPreferences
import com.focusdial.app.data.FocusState
import com.focusdial.app.data.HistoryRepository
import com.focusdial.app.theme.FocusTheme
import com.focusdial.app.theme.FocusThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

class FocusWatchFaceService : WatchFaceService() {

    override fun createUserStyleSchema(): UserStyleSchema {
        return UserStyleSchema(emptyList())
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(emptyList(), currentUserStyleRepository)
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = FocusCanvasRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE
        )
        val watchFace = WatchFace(WatchFaceType.DIGITAL, renderer)
        watchFace.setTapListener(object : WatchFace.TapListener {
            override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
                if (tapType == TapType.UP && complicationSlot == null) {
                    val intent = Intent(applicationContext, ToggleSessionActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    applicationContext.startActivity(intent)
                }
            }
        })
        return watchFace
    }
}

class FocusCanvasRenderer(
    private val context: android.content.Context,
    surfaceHolder: SurfaceHolder,
    private val watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<Renderer.SharedAssets>(
    surfaceHolder = surfaceHolder,
    currentUserStyleRepository = currentUserStyleRepository,
    watchState = watchState,
    canvasType = canvasType,
    interactiveDrawModeUpdateDelayMillis = 16L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = true
) {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = FocusPreferences(context)
    private val historyRepository = HistoryRepository(context)
    private val calendarHelper = CalendarHelper(context)

    private val is24Hour: Boolean
        get() = DateFormat.is24HourFormat(context)

    // Cached data — written by background coroutine, read by render()
    @Volatile private var cachedTotalText = ""
    @Volatile private var cachedDots = ""
    @Volatile private var cachedWeeklyText = ""
    @Volatile private var cachedScoreText = ""
    @Volatile private var cachedStreakText = ""
    @Volatile private var cachedCalendarHint: String? = null
    @Volatile private var cachedTheme: FocusTheme = FocusThemes.MINIMAL

    // Paints — reconfigured when theme changes
    private var currentThemeId = "minimal"

    private val timePaint = Paint().apply {
        color = Color.WHITE
        textSize = 72f
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val statusPaint = Paint().apply {
        color = Color.WHITE
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        letterSpacing = 0.2f
    }

    private val infoPaint = Paint().apply {
        color = Color.parseColor("#CCCCCC")
        textSize = 17f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val subtlePaint = Paint().apply {
        color = Color.parseColor("#888888")
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val arcPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val trackPaint = Paint().apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val datePaint = Paint().apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val ambientTimePaint = Paint().apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 64f
        typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = false
    }

    private val ambientInfoPaint = Paint().apply {
        color = Color.parseColor("#666666")
        textSize = 14f
        textAlign = Paint.Align.CENTER
        isAntiAlias = false
    }

    init {
        backgroundScope.launch {
            while (true) {
                refreshCacheAsync()
                delay(5000L)
            }
        }
    }

    private suspend fun refreshCacheAsync() {
        val manager = FocusSessionManager.getInstance(context)

        val theme = FocusThemes.getById(preferences.getSelectedTheme())
        cachedTheme = theme
        if (theme.id != currentThemeId) {
            currentThemeId = theme.id
            applyTheme(theme)
        }

        val totalMillis = preferences.getDailyTotalMillis() +
            if (manager.state.value is FocusState.Focus) manager.elapsedFocusMillis else 0L
        val totalMin = (totalMillis / 60_000).toInt()
        val hours = totalMin / 60
        val minutes = totalMin % 60
        cachedTotalText = if (hours > 0) "${hours}h ${minutes}m deep" else "${minutes}m deep"

        val completed = preferences.getCompletedSessions()
        val goal = preferences.getDailyGoal()
        cachedDots = buildString {
            repeat(completed.coerceAtMost(goal)) { append("●") }
            repeat((goal - completed).coerceAtLeast(0)) { append("○") }
        }

        val weeklyMillis = historyRepository.getWeeklyTotalMillis()
        val weeklyHours = (weeklyMillis / 3_600_000).toInt()
        val weeklyMin = ((weeklyMillis % 3_600_000) / 60_000).toInt()
        cachedWeeklyText = if (weeklyHours > 0) "This week: ${weeklyHours}h ${weeklyMin}m" else "This week: ${weeklyMin}m"

        val score = historyRepository.getDailyAverageScore()
        cachedScoreText = if (score > 0) "Score: $score" else ""

        val streak = historyRepository.getCurrentStreak()
        cachedStreakText = if (streak > 0) "${streak}d streak" else ""

        try {
            val event = calendarHelper.getNextEvent()
            cachedCalendarHint = calendarHelper.formatEventHint(event)
        } catch (_: Exception) {
            cachedCalendarHint = null
        }
    }

    private fun applyTheme(theme: FocusTheme) {
        timePaint.typeface = Typeface.create(theme.timeFontFamily, theme.timeFontWeight)
        statusPaint.textSize = theme.statusFontSize
        arcPaint.strokeWidth = theme.arcStrokeWidth
    }

    class FocusSharedAssets : SharedAssets {
        override fun onDestroy() {}
    }

    override suspend fun createSharedAssets(): SharedAssets = FocusSharedAssets()

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: SharedAssets) {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        canvas.drawColor(Color.BLACK)

        val isAmbient = renderParameters.drawMode == androidx.wear.watchface.DrawMode.AMBIENT

        if (isAmbient) {
            renderAmbient(canvas, centerX, centerY, zonedDateTime)
            return
        }

        val theme = cachedTheme
        val manager = FocusSessionManager.getInstance(context)
        val state = manager.state.value

        when (state) {
            is FocusState.Focus -> renderFocusMode(canvas, centerX, centerY, width, height, zonedDateTime, manager, theme)
            is FocusState.Break -> renderBreakMode(canvas, centerX, centerY, width, height, zonedDateTime, manager, theme)
            is FocusState.Idle -> renderIdleMode(canvas, centerX, centerY, width, height, zonedDateTime, theme)
        }
    }

    private fun formatTime(time: ZonedDateTime): String {
        return if (is24Hour) {
            String.format("%02d:%02d", time.hour, time.minute)
        } else {
            val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
            String.format("%d:%02d", hour, time.minute)
        }
    }

    private fun formatDate(time: ZonedDateTime): String {
        val day = time.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val month = time.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$day, ${time.dayOfMonth} $month"
    }

    private fun renderFocusMode(
        canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float,
        time: ZonedDateTime, manager: FocusSessionManager, theme: FocusTheme
    ) {
        val progress = manager.progressFraction
        val elapsedMin = (manager.elapsedFocusMillis / 60_000).toInt()
        val remainingMin = (manager.remainingFocusMillis / 60_000).toInt()
        val state = manager.state.value as FocusState.Focus

        val pad = theme.arcStrokeWidth + 20f
        val arcRect = RectF(pad, pad, w - pad, h - pad)
        trackPaint.color = theme.trackColor
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)
        arcPaint.color = theme.accentColor
        canvas.drawArc(arcRect, -90f, progress * 360f, false, arcPaint)

        statusPaint.color = theme.accentColor
        canvas.drawText("FOCUS", cx, cy - 85f, statusPaint)

        timePaint.textSize = 68f
        canvas.drawText("${elapsedMin}min", cx, cy - 10f, timePaint)
        timePaint.textSize = 72f

        canvas.drawText("Break in ${remainingMin}min", cx, cy + 35f, infoPaint)

        val intText = if (state.interruptionCount == 0) "0 interruptions"
            else "${state.interruptionCount} interruption${if (state.interruptionCount > 1) "s" else ""}"
        canvas.drawText(intText, cx, cy + 70f, subtlePaint)

        canvas.drawText(formatTime(time), cx, cy + 120f, subtlePaint)
    }

    private val breathPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private fun renderBreakMode(
        canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float,
        time: ZonedDateTime, manager: FocusSessionManager, theme: FocusTheme
    ) {
        val state = manager.state.value as FocusState.Break
        val elapsed = System.currentTimeMillis() - state.startTimeMillis
        val progress = (elapsed.toFloat() / state.durationMillis).coerceIn(0f, 1f)
        val remainingMin = (manager.remainingBreakMillis / 60_000).toInt()

        val pad = theme.arcStrokeWidth + 20f
        val arcRect = RectF(pad, pad, w - pad, h - pad)
        trackPaint.color = theme.trackColor
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)
        arcPaint.color = theme.breakColor
        canvas.drawArc(arcRect, -90f, progress * 360f, false, arcPaint)

        // Breathing circle animation (4s inhale + 4s exhale cycle)
        val breathCycle = 8000L
        val breathPhase = (System.currentTimeMillis() % breathCycle).toFloat() / breathCycle
        val breathScale = if (breathPhase < 0.5f) {
            breathPhase * 2f
        } else {
            1f - (breathPhase - 0.5f) * 2f
        }
        val minRadius = 20f
        val maxRadius = 50f
        val breathRadius = minRadius + (maxRadius - minRadius) * breathScale
        breathPaint.color = theme.breakColor
        breathPaint.alpha = (100 + 155 * breathScale).toInt()
        canvas.drawCircle(cx, cy - 30f, breathRadius, breathPaint)

        val breathLabel = if (breathPhase < 0.5f) "Breathe in" else "Breathe out"
        infoPaint.color = theme.breakColor
        canvas.drawText(breathLabel, cx, cy + 40f, infoPaint)
        infoPaint.color = Color.parseColor("#CCCCCC")

        statusPaint.color = theme.breakColor
        canvas.drawText("BREAK", cx, cy - 90f, statusPaint)

        timePaint.textSize = 44f
        val breakText = if (remainingMin > 0) "${remainingMin}min" else "Done!"
        canvas.drawText(breakText, cx, cy + 75f, timePaint)
        timePaint.textSize = 72f

        dotPaint.color = theme.accentColor
        canvas.drawText(cachedDots, cx, cy + 110f, dotPaint)

        canvas.drawText(formatTime(time), cx, cy + 140f, subtlePaint)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun renderIdleMode(
        canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float,
        time: ZonedDateTime, theme: FocusTheme
    ) {
        timePaint.textSize = 72f
        canvas.drawText(formatTime(time), cx, cy - 55f, timePaint)

        canvas.drawText(formatDate(time), cx, cy - 15f, datePaint)

        infoPaint.color = Color.parseColor("#CCCCCC")
        canvas.drawText(cachedWeeklyText, cx, cy + 25f, infoPaint)

        if (cachedScoreText.isNotEmpty()) {
            canvas.drawText(cachedScoreText, cx, cy + 50f, infoPaint)
        }

        // Dots + streak on same line
        dotPaint.color = theme.accentColor
        val dotsAndStreak = if (cachedStreakText.isNotEmpty()) {
            "$cachedDots  $cachedStreakText"
        } else {
            cachedDots
        }
        canvas.drawText(dotsAndStreak, cx, cy + 80f, dotPaint)

        // Calendar hint
        val hint = cachedCalendarHint
        if (hint != null) {
            subtlePaint.color = Color.parseColor("#AAAAAA")
            canvas.drawText(hint, cx, cy + 110f, subtlePaint)
            subtlePaint.color = Color.parseColor("#888888")
        } else {
            subtlePaint.color = Color.parseColor("#666666")
            canvas.drawText("Tap to focus", cx, cy + 110f, subtlePaint)
            subtlePaint.color = Color.parseColor("#888888")
        }
    }

    private fun renderAmbient(canvas: Canvas, cx: Float, cy: Float, time: ZonedDateTime) {
        val manager = FocusSessionManager.getInstance(context)
        val state = manager.state.value

        canvas.drawText(formatTime(time), cx, cy - 10f, ambientTimePaint)
        canvas.drawText(formatDate(time), cx, cy + 25f, ambientInfoPaint)

        when (state) {
            is FocusState.Focus -> {
                val elapsedMin = (manager.elapsedFocusMillis / 60_000).toInt()
                canvas.drawText("FOCUS ${elapsedMin}min", cx, cy + 60f, ambientInfoPaint)
            }
            is FocusState.Break -> {
                val remainingMin = (manager.remainingBreakMillis / 60_000).toInt()
                canvas.drawText("BREAK ${remainingMin}min", cx, cy + 60f, ambientInfoPaint)
            }
            is FocusState.Idle -> {
                canvas.drawText(cachedTotalText, cx, cy + 60f, ambientInfoPaint)
            }
        }
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: SharedAssets) {
        canvas.drawColor(Color.argb(64, 0, 0, 0))
    }

    override fun onDestroy() {
        backgroundScope.cancel()
        super.onDestroy()
    }
}
