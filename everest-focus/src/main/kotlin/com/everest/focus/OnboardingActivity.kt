package com.everest.focus

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.everest.focus.data.FocusPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnboardingActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var prefs: FocusPreferences
    private var currentPage = 0

    private val pages = listOf(
        Page("Your Watch Face\nTracks Focus", "See time and focus progress\nin one glance"),
        Page("Tap to Start", "One tap on the dial\nstarts a focus session"),
        Page("Build Your Streak", "Set a daily goal and\nwatch your streak grow")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = FocusPreferences(this)
        showPage()
    }

    private fun showPage() {
        val page = pages[currentPage]
        val isLast = currentPage == pages.lastIndex

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 60, 32, 40)
            setBackgroundColor(Color.BLACK)
        }

        layout.addView(TextView(this).apply {
            text = page.title
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24
            )
        })

        layout.addView(TextView(this).apply {
            text = page.subtitle
            textSize = 14f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
        })

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 40
            )
        })

        // Dots indicator
        layout.addView(TextView(this).apply {
            text = buildString {
                repeat(pages.size) { i ->
                    append(if (i == currentPage) "●" else "○")
                    if (i < pages.lastIndex) append(" ")
                }
            }
            textSize = 14f
            setTextColor(Color.parseColor("#4CAF50"))
            gravity = Gravity.CENTER
        })

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24
            )
        })

        layout.addView(Button(this).apply {
            text = if (isLast) "Get Started" else "Next"
            textSize = 14f
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (isLast) {
                    finishOnboarding()
                } else {
                    currentPage++
                    showPage()
                }
            }
        })

        setContentView(layout)
    }

    private fun finishOnboarding() {
        scope.launch {
            prefs.setOnboardingComplete()
        }
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }

    private data class Page(val title: String, val subtitle: String)
}
