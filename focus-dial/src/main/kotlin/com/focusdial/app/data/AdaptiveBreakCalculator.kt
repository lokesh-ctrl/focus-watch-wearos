package com.focusdial.app.data

object AdaptiveBreakCalculator {

    data class BreakSuggestion(
        val durationMinutes: Int,
        val reason: String
    )

    fun suggest(
        baseBreakMinutes: Int,
        consecutiveSessionsToday: Int,
        currentHour: Int
    ): BreakSuggestion {
        val baseDuration = baseBreakMinutes.coerceIn(3, 30)

        if (consecutiveSessionsToday >= 3) {
            return BreakSuggestion(
                durationMinutes = (baseDuration * 2).coerceIn(3, 30),
                reason = "Extended break ($consecutiveSessionsToday sessions)"
            )
        }

        if (currentHour >= 20) {
            return BreakSuggestion(
                durationMinutes = (baseDuration * 2).coerceIn(3, 30),
                reason = "Evening wind-down"
            )
        }

        return BreakSuggestion(
            durationMinutes = baseDuration,
            reason = "Standard break"
        )
    }
}
