package com.focusdial.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.focusdial.app.data.BillingManager
import com.focusdial.app.data.FocusPreferences
import com.focusdial.app.data.FocusProfile
import com.focusdial.app.data.HistoryRepository
import com.focusdial.app.theme.FocusThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : Activity() {

    private lateinit var prefs: FocusPreferences
    private lateinit var historyRepo: HistoryRepository
    private lateinit var billingManager: BillingManager
    private val scope = CoroutineScope(Dispatchers.Main)

    private var focusMinutes = 50
    private var breakMinutes = 10
    private var dailyGoal = 4
    private var selectedTheme = "minimal"
    private var adaptiveBreaks = true
    private var calendarEnabled = false
    private var isPro = false
    private var dndEnabled = false
    private var activeProfileId = ""
    private var hapticStyle = "gentle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = FocusPreferences(this)
        historyRepo = HistoryRepository(this)
        billingManager = BillingManager.getInstance(this)

        scope.launch {
            focusMinutes = prefs.getFocusDuration()
            breakMinutes = prefs.getBreakDuration()
            dailyGoal = prefs.getDailyGoal()
            selectedTheme = prefs.getSelectedTheme()
            adaptiveBreaks = prefs.isAdaptiveBreaksEnabled()
            calendarEnabled = prefs.isCalendarEnabled()
            isPro = prefs.isPro()
            dndEnabled = prefs.isDndEnabled()
            activeProfileId = prefs.getActiveProfile()
            hapticStyle = prefs.getHapticStyle()
            buildUi()
        }
    }

    private fun buildUi() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 40, 24, 40)
        }

        // Title
        layout.addView(sectionTitle("Settings"))
        layout.addView(spacer())

        // Theme Picker
        layout.addView(label("Theme"))
        layout.addView(createThemePicker())
        layout.addView(spacer())

        // Focus Profiles (Pro)
        layout.addView(proLabel("Focus Profiles"))
        layout.addView(createProfilePicker())
        layout.addView(hintText("Quick presets for different work styles"))
        layout.addView(spacer())

        // Focus Duration
        layout.addView(label("Focus Duration"))
        val focusValue = valueText("${focusMinutes}min")
        layout.addView(focusValue)
        layout.addView(createButtonRow(
            onMinus = {
                focusMinutes = (focusMinutes - 5).coerceAtLeast(15)
                focusValue.text = "${focusMinutes}min"
                save()
            },
            onPlus = {
                focusMinutes = (focusMinutes + 5).coerceAtMost(120)
                focusValue.text = "${focusMinutes}min"
                save()
            }
        ))
        layout.addView(spacer())

        // Break Duration
        layout.addView(label("Break Duration"))
        val breakValue = valueText("${breakMinutes}min")
        layout.addView(breakValue)
        layout.addView(createButtonRow(
            onMinus = {
                breakMinutes = (breakMinutes - 1).coerceAtLeast(3)
                breakValue.text = "${breakMinutes}min"
                save()
            },
            onPlus = {
                breakMinutes = (breakMinutes + 1).coerceAtMost(30)
                breakValue.text = "${breakMinutes}min"
                save()
            }
        ))
        layout.addView(spacer())

        // Daily Goal
        layout.addView(label("Daily Goal"))
        val goalValue = valueText("$dailyGoal sessions")
        layout.addView(goalValue)
        layout.addView(createButtonRow(
            onMinus = {
                dailyGoal = (dailyGoal - 1).coerceAtLeast(1)
                goalValue.text = "$dailyGoal sessions"
                save()
            },
            onPlus = {
                dailyGoal = (dailyGoal + 1).coerceAtMost(8)
                goalValue.text = "$dailyGoal sessions"
                save()
            }
        ))
        layout.addView(spacer())

        // Adaptive Breaks Toggle (Pro)
        layout.addView(proLabel("Adaptive Breaks"))
        val adaptiveSwitch = Switch(this).apply {
            isChecked = adaptiveBreaks
            isEnabled = isPro
            setOnCheckedChangeListener { _, checked ->
                if (!isPro) {
                    isChecked = false
                    launchUpgrade()
                    return@setOnCheckedChangeListener
                }
                adaptiveBreaks = checked
                scope.launch { prefs.setAdaptiveBreaksEnabled(checked) }
            }
        }
        layout.addView(adaptiveSwitch)
        layout.addView(hintText("Extends breaks after 3+ sessions or late hours"))
        layout.addView(spacer())

        // Auto-DND Toggle (Pro)
        layout.addView(proLabel("Auto Do Not Disturb"))
        val dndSwitch = Switch(this).apply {
            isChecked = dndEnabled
            isEnabled = isPro
            setOnCheckedChangeListener { _, checked ->
                if (!isPro) {
                    isChecked = false
                    launchUpgrade()
                    return@setOnCheckedChangeListener
                }
                dndEnabled = checked
                scope.launch { prefs.setDndEnabled(checked) }
            }
        }
        layout.addView(dndSwitch)
        layout.addView(hintText("Silences notifications during focus sessions"))
        layout.addView(spacer())

        // Haptic Style
        layout.addView(label("Haptics"))
        layout.addView(createHapticPicker())
        layout.addView(spacer())

        // Calendar Toggle
        layout.addView(label("Calendar Awareness"))
        val calendarSwitch = Switch(this).apply {
            isChecked = calendarEnabled
            setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    requestCalendarPermission()
                }
                calendarEnabled = checked
                scope.launch { prefs.setCalendarEnabled(checked) }
            }
        }
        layout.addView(calendarSwitch)
        layout.addView(hintText("Shows upcoming meetings, suggests shorter focus"))
        layout.addView(spacer())

        // Stats Section
        layout.addView(sectionTitle("Stats"))
        layout.addView(spacer())

        val streakText = valueText("")
        val weeklyText = valueText("")
        val scoreText = valueText("")
        layout.addView(streakText)
        layout.addView(weeklyText)
        layout.addView(scoreText)

        scope.launch {
            val streak = historyRepo.getCurrentStreak()
            val weeklyMillis = historyRepo.getWeeklyTotalMillis()
            val weeklyH = (weeklyMillis / 3_600_000).toInt()
            val weeklyM = ((weeklyMillis % 3_600_000) / 60_000).toInt()
            val avgScore = historyRepo.getDailyAverageScore()

            streakText.text = "Streak: ${streak}d"
            weeklyText.text = if (weeklyH > 0) "Weekly: ${weeklyH}h ${weeklyM}m" else "Weekly: ${weeklyM}m"
            scoreText.text = if (avgScore > 0) "Avg Score: $avgScore" else "Avg Score: —"
        }

        // Upgrade Button (only for free users)
        if (!isPro) {
            layout.addView(spacer())
            layout.addView(Button(this).apply {
                text = "Upgrade to Pro"
                textSize = 14f
                setBackgroundColor(0xFF4CAF50.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setOnClickListener { launchUpgrade() }
                minimumHeight = 56
            })
            layout.addView(hintText("Unlock all themes, adaptive breaks, auto-DND & more"))
        }

        val scrollView = ScrollView(this).apply {
            addView(layout)
            setBackgroundColor(0xFF000000.toInt())
        }

        setContentView(scrollView)
    }

    private fun createThemePicker(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)

            FocusThemes.ALL.forEach { theme ->
                val isFreeTheme = theme.id == "minimal"
                val isLocked = !isPro && !isFreeTheme
                val size = 40
                val circle = View(this@SettingsActivity).apply {
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isLocked) 0xFF333333.toInt() else theme.accentColor)
                        if (theme.id == selectedTheme) {
                            setStroke(4, 0xFFFFFFFF.toInt())
                        } else {
                            setStroke(2, 0xFF666666.toInt())
                        }
                    }
                    background = drawable
                    alpha = if (isLocked) 0.5f else 1.0f
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(12, 0, 12, 0)
                    }
                    setOnClickListener {
                        if (isLocked) {
                            launchUpgrade()
                            return@setOnClickListener
                        }
                        selectedTheme = theme.id
                        scope.launch { prefs.setSelectedTheme(theme.id) }
                        recreate()
                    }
                }
                addView(circle)
            }
        }
    }

    private fun createProfilePicker(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)

            FocusProfile.ALL.forEach { profile ->
                addView(Button(this@SettingsActivity).apply {
                    text = profile.name
                    textSize = 11f
                    val isActive = activeProfileId == profile.id
                    setBackgroundColor(if (isActive) 0xFF4CAF50.toInt() else 0xFF333333.toInt())
                    setTextColor(0xFFFFFFFF.toInt())
                    minimumWidth = 0
                    minimumHeight = 40
                    setPadding(12, 4, 12, 4)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(4, 0, 4, 0) }
                    layoutParams = params
                    setOnClickListener {
                        if (!isPro) {
                            launchUpgrade()
                            return@setOnClickListener
                        }
                        activeProfileId = profile.id
                        focusMinutes = profile.focusMinutes
                        breakMinutes = profile.breakMinutes
                        scope.launch {
                            prefs.setActiveProfile(profile.id)
                            prefs.setFocusDuration(profile.focusMinutes)
                            prefs.setBreakDuration(profile.breakMinutes)
                        }
                        recreate()
                    }
                })
            }
        }
    }

    private fun createHapticPicker(): LinearLayout {
        val styles = listOf("gentle", "assertive", "silent")
        val labels = listOf("Gentle", "Strong", "Silent")
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)

            styles.forEachIndexed { index, style ->
                addView(Button(this@SettingsActivity).apply {
                    text = labels[index]
                    textSize = 11f
                    val isActive = hapticStyle == style
                    setBackgroundColor(if (isActive) 0xFF4CAF50.toInt() else 0xFF333333.toInt())
                    setTextColor(0xFFFFFFFF.toInt())
                    minimumWidth = 0
                    minimumHeight = 40
                    setPadding(12, 4, 12, 4)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(4, 0, 4, 0) }
                    setOnClickListener {
                        hapticStyle = style
                        scope.launch { prefs.setHapticStyle(style) }
                        recreate()
                    }
                })
            }
        }
    }

    private fun createButtonRow(onMinus: () -> Unit, onPlus: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 4)

            addView(Button(this@SettingsActivity).apply {
                text = "−"
                textSize = 20f
                setOnClickListener { onMinus() }
                minimumWidth = 80
                minimumHeight = 48
            })

            addView(Button(this@SettingsActivity).apply {
                text = "+"
                textSize = 20f
                setOnClickListener { onPlus() }
                minimumWidth = 80
                minimumHeight = 48
            })
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
    }

    private fun valueText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(0xFF4CAF50.toInt())
            gravity = Gravity.CENTER
        }
    }

    private fun hintText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        }
    }

    private fun spacer(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16
            )
        }
    }

    private fun proLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = if (isPro) text else "$text [PRO]"
            textSize = 16f
            setTextColor(if (isPro) 0xFFFFFFFF.toInt() else 0xFFFFD700.toInt())
            gravity = Gravity.CENTER
        }
    }

    private fun launchUpgrade() {
        scope.launch {
            billingManager.launchPurchaseFlow(this@SettingsActivity)
        }
    }

    private fun requestCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_CALENDAR), 100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            calendarEnabled = false
            scope.launch { prefs.setCalendarEnabled(false) }
            recreate()
        }
    }

    private fun save() {
        scope.launch {
            prefs.setFocusDuration(focusMinutes)
            prefs.setBreakDuration(breakMinutes)
            prefs.setDailyGoal(dailyGoal)
        }
    }
}
