package com.everest.focus.data

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
        if (consecutiveSessionsToday >= 3) {
            return BreakSuggestion(
                durationMinutes = baseBreakMinutes * 2,
                reason = "Extended break ($consecutiveSessionsToday sessions)"
            )
        }

        if (currentHour >= 20) {
            return BreakSuggestion(
                durationMinutes = baseBreakMinutes * 2,
                reason = "Evening wind-down"
            )
        }

        return BreakSuggestion(
            durationMinutes = baseBreakMinutes,
            reason = "Standard break"
        )
    }
}
