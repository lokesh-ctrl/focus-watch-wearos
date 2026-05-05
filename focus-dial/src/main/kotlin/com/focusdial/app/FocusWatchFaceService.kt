package com.focusdial.app

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
                    renderer.handleTap(tapEvent.yPos.toFloat())
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

    // Overlay state
    private enum class OverlayState { HIDDEN, SHOWING }
    @Volatile private var overlayState = OverlayState.HIDDEN
    @Volatile private var overlayShownAt = 0L
    private val overlayDurationMs = 3000L

    // Cached data
    @Volatile private var cachedTotalText = ""
    @Volatile private var cachedDots = ""
    @Volatile private var cachedWeeklyText = ""
    @Volatile private var cachedScoreText = ""
    @Volatile private var cachedStreakText = ""
    @Volatile private var cachedCalendarHint: String? = null
    @Volatile private var cachedTheme: FocusTheme = FocusThemes.MINIMAL

    private var currentThemeId = "minimal"

    // Paints
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

    private val breathPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val overlayScrimPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        isAntiAlias = true
    }

    private val overlayTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val overlayIconPaint = Paint().apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val overlayDividerPaint = Paint().apply {
        color = Color.parseColor("#555555")
        strokeWidth = 1f
        isAntiAlias = true
    }

    init {
        backgroundScope.launch {
            while (true) {
                refreshCacheAsync()
                delay(5000L)
            }
        }
    }

    fun handleTap(yPos: Float) {
        val manager = FocusSessionManager.getInstance(context)
        val state = manager.state.value
        val centerY = surfaceHolder.surfaceFrame.height() / 2f

        when {
            // IDLE: start immediately
            state is FocusState.Idle -> manager.startFocus()

            // PAUSED: resume immediately
            state is FocusState.Paused -> manager.resumeSession()

            // FOCUS with overlay showing: handle pause/stop zones
            state is FocusState.Focus && overlayState == OverlayState.SHOWING -> {
                if (yPos < centerY) {
                    manager.pauseSession()
                } else {
                    manager.stopSession()
                }
                overlayState = OverlayState.HIDDEN
            }

            // FOCUS without overlay: show overlay
            state is FocusState.Focus -> {
                overlayState = OverlayState.SHOWING
                overlayShownAt = System.currentTimeMillis()
            }

            // BREAK with overlay showing: end break
            state is FocusState.Break && overlayState == OverlayState.SHOWING -> {
                manager.stopSession()
                overlayState = OverlayState.HIDDEN
            }

            // BREAK without overlay: show overlay
            state is FocusState.Break -> {
                overlayState = OverlayState.SHOWING
                overlayShownAt = System.currentTimeMillis()
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
            overlayState = OverlayState.HIDDEN
            renderAmbient(canvas, centerX, centerY, zonedDateTime)
            return
        }

        // Auto-dismiss overlay after timeout
        if (overlayState == OverlayState.SHOWING &&
            System.currentTimeMillis() - overlayShownAt > overlayDurationMs) {
            overlayState = OverlayState.HIDDEN
        }

        val theme = cachedTheme
        val manager = FocusSessionManager.getInstance(context)
        val state = manager.state.value

        when (state) {
            is FocusState.Focus -> renderFocusMode(canvas, centerX, centerY, width, height, zonedDateTime, manager, theme)
            is FocusState.Paused -> renderPausedMode(canvas, centerX, centerY, width, height, zonedDateTime, state, theme)
            is FocusState.Break -> renderBreakMode(canvas, centerX, centerY, width, height, zonedDateTime, manager, theme)
            is FocusState.Idle -> renderIdleMode(canvas, centerX, centerY, width, height, zonedDateTime, theme)
        }

        // Render overlay on top if active
        if (overlayState == OverlayState.SHOWING && (state is FocusState.Focus || state is FocusState.Break)) {
            renderOverlay(canvas, centerX, centerY, width, height, state)
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

    private fun renderPausedMode(
        canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float,
        time: ZonedDateTime, state: FocusState.Paused, theme: FocusTheme
    ) {
        val progress = (state.elapsedBeforePauseMillis.toFloat() / state.durationMillis).coerceIn(0f, 1f)
        val elapsedMin = (state.elapsedBeforePauseMillis / 60_000).toInt()
        val pausedForSec = ((System.currentTimeMillis() - state.pausedAtMillis) / 1000).toInt()
        val pausedMin = pausedForSec / 60
        val pausedSecRemainder = pausedForSec % 60

        // Desaturated arc — dashed appearance via short segments
        val pad = theme.arcStrokeWidth + 20f
        val arcRect = RectF(pad, pad, w - pad, h - pad)
        trackPaint.color = theme.trackColor
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // Draw progress arc in muted color
        val mutedAccent = Color.argb(
            128,
            Color.red(theme.accentColor),
            Color.green(theme.accentColor),
            Color.blue(theme.accentColor)
        )
        arcPaint.color = mutedAccent
        canvas.drawArc(arcRect, -90f, progress * 360f, false, arcPaint)

        // PAUSED label with pulsing alpha
        val pulsePhase = (System.currentTimeMillis() % 2000).toFloat() / 2000f
        val pulseAlpha = (155 + 100 * kotlin.math.sin(pulsePhase * 2 * Math.PI).toFloat()).toInt()
        statusPaint.color = Color.argb(pulseAlpha, 255, 193, 7) // amber
        canvas.drawText("PAUSED", cx, cy - 85f, statusPaint)

        timePaint.textSize = 68f
        timePaint.color = Color.parseColor("#CCCCCC")
        canvas.drawText("${elapsedMin}min", cx, cy - 10f, timePaint)
        timePaint.color = Color.WHITE
        timePaint.textSize = 72f

        // Pulsing "Tap to resume" text
        infoPaint.color = Color.argb(pulseAlpha, 255, 255, 255)
        canvas.drawText("Tap to resume", cx, cy + 40f, infoPaint)
        infoPaint.color = Color.parseColor("#CCCCCC")

        // Paused duration
        val pausedText = if (pausedMin > 0) "paused ${pausedMin}m ${pausedSecRemainder}s"
            else "paused ${pausedSecRemainder}s"
        subtlePaint.color = Color.parseColor("#FF9800")
        canvas.drawText(pausedText, cx, cy + 75f, subtlePaint)
        subtlePaint.color = Color.parseColor("#888888")

        // Interruption count
        canvas.drawText("${state.interruptionCount} interruption${if (state.interruptionCount != 1) "s" else ""}", cx, cy + 105f, subtlePaint)

        canvas.drawText(formatTime(time), cx, cy + 140f, subtlePaint)
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

        // Breathing circle animation
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

        dotPaint.color = theme.accentColor
        val dotsAndStreak = if (cachedStreakText.isNotEmpty()) {
            "$cachedDots  $cachedStreakText"
        } else {
            cachedDots
        }
        canvas.drawText(dotsAndStreak, cx, cy + 80f, dotPaint)

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

    private fun renderOverlay(
        canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float,
        state: FocusState
    ) {
        // Semi-transparent scrim
        canvas.drawCircle(cx, cy, w / 2f, overlayScrimPaint)

        // Divider line
        canvas.drawLine(cx - 80f, cy, cx + 80f, cy, overlayDividerPaint)

        if (state is FocusState.Focus) {
            // Top zone: PAUSE
            overlayIconPaint.color = Color.parseColor("#FFC107") // amber
            canvas.drawText("⏸", cx, cy - 50f, overlayIconPaint)
            overlayTextPaint.color = Color.parseColor("#FFC107")
            canvas.drawText("PAUSE", cx, cy - 20f, overlayTextPaint)

            // Bottom zone: STOP
            overlayIconPaint.color = Color.parseColor("#F44336") // red
            canvas.drawText("■", cx, cy + 50f, overlayIconPaint)
            overlayTextPaint.color = Color.parseColor("#F44336")
            canvas.drawText("STOP", cx, cy + 80f, overlayTextPaint)
        } else if (state is FocusState.Break) {
            // Centered: End break
            overlayTextPaint.color = Color.WHITE
            canvas.drawText("End break?", cx, cy - 10f, overlayTextPaint)
            overlayTextPaint.color = Color.parseColor("#FFC107")
            canvas.drawText("Tap to confirm", cx, cy + 20f, overlayTextPaint)
        }

        // Timeout indicator — subtle fade hint
        val elapsed = System.currentTimeMillis() - overlayShownAt
        val remaining = (overlayDurationMs - elapsed).toFloat() / overlayDurationMs
        val indicatorPaint = Paint().apply {
            color = Color.parseColor("#666666")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        val indicatorRect = RectF(cx - 30f, cy + 110f, cx + 30f, cy + 116f)
        canvas.drawRoundRect(indicatorRect, 3f, 3f, indicatorPaint)
        indicatorPaint.color = Color.WHITE
        val filledWidth = 60f * remaining
        val filledRect = RectF(cx - 30f, cy + 110f, cx - 30f + filledWidth, cy + 116f)
        canvas.drawRoundRect(filledRect, 3f, 3f, indicatorPaint)
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
            is FocusState.Paused -> {
                val elapsedMin = (state.elapsedBeforePauseMillis / 60_000).toInt()
                canvas.drawText("PAUSED ${elapsedMin}min", cx, cy + 60f, ambientInfoPaint)
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
