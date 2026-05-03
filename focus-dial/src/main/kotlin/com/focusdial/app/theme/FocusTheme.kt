package com.focusdial.app.theme

import android.graphics.Color
import android.graphics.Typeface

data class FocusTheme(
    val id: String,
    val displayName: String,
    val accentColor: Int,
    val breakColor: Int,
    val trackColor: Int,
    val arcStrokeWidth: Float,
    val timeFontWeight: Int,
    val timeFontFamily: String,
    val statusFontSize: Float
)

object FocusThemes {
    val MINIMAL = FocusTheme(
        id = "minimal",
        displayName = "Minimal",
        accentColor = Color.parseColor("#4CAF50"),
        breakColor = Color.parseColor("#FF9800"),
        trackColor = Color.parseColor("#333333"),
        arcStrokeWidth = 10f,
        timeFontWeight = Typeface.NORMAL,
        timeFontFamily = "sans-serif-light",
        statusFontSize = 18f
    )

    val EMBER = FocusTheme(
        id = "ember",
        displayName = "Ember",
        accentColor = Color.parseColor("#FF5722"),
        breakColor = Color.parseColor("#FFC107"),
        trackColor = Color.parseColor("#3E2723"),
        arcStrokeWidth = 14f,
        timeFontWeight = Typeface.BOLD,
        timeFontFamily = "sans-serif-medium",
        statusFontSize = 18f
    )

    val OCEAN = FocusTheme(
        id = "ocean",
        displayName = "Ocean",
        accentColor = Color.parseColor("#00BCD4"),
        breakColor = Color.parseColor("#26C6DA"),
        trackColor = Color.parseColor("#1A237E"),
        arcStrokeWidth = 6f,
        timeFontWeight = Typeface.NORMAL,
        timeFontFamily = "sans-serif-thin",
        statusFontSize = 16f
    )

    val MONOCHROME = FocusTheme(
        id = "monochrome",
        displayName = "Monochrome",
        accentColor = Color.parseColor("#FFFFFF"),
        breakColor = Color.parseColor("#CCCCCC"),
        trackColor = Color.parseColor("#444444"),
        arcStrokeWidth = 8f,
        timeFontWeight = Typeface.NORMAL,
        timeFontFamily = "sans-serif-light",
        statusFontSize = 17f
    )

    val FOREST = FocusTheme(
        id = "forest",
        displayName = "Forest",
        accentColor = Color.parseColor("#2E7D32"),
        breakColor = Color.parseColor("#81C784"),
        trackColor = Color.parseColor("#1B5E20"),
        arcStrokeWidth = 12f,
        timeFontWeight = Typeface.NORMAL,
        timeFontFamily = "sans-serif-medium",
        statusFontSize = 17f
    )

    val NEON = FocusTheme(
        id = "neon",
        displayName = "Neon",
        accentColor = Color.parseColor("#E040FB"),
        breakColor = Color.parseColor("#00E5FF"),
        trackColor = Color.parseColor("#1A1A2E"),
        arcStrokeWidth = 8f,
        timeFontWeight = Typeface.BOLD,
        timeFontFamily = "sans-serif-condensed",
        statusFontSize = 18f
    )

    val ALL = listOf(MINIMAL, EMBER, OCEAN, MONOCHROME, FOREST, NEON)

    fun getById(id: String): FocusTheme = ALL.find { it.id == id } ?: MINIMAL
}
