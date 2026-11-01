package com.focusdial.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveBreakCalculatorTest {

    @Test
    fun suggest_returnsStandardBreakUnchanged() {
        val suggestion = AdaptiveBreakCalculator.suggest(
            baseBreakMinutes = 5,
            consecutiveSessionsToday = 0,
            currentHour = 9
        )

        assertEquals(5, suggestion.durationMinutes)
        assertEquals("Standard break", suggestion.reason)
    }

    @Test
    fun suggest_extendsBreakAfterThreeSessionsButCapsAtThirtyMinutes() {
        val suggestion = AdaptiveBreakCalculator.suggest(
            baseBreakMinutes = 20,
            consecutiveSessionsToday = 3,
            currentHour = 10
        )

        assertEquals(30, suggestion.durationMinutes)
        assertEquals("Extended break (3 sessions)", suggestion.reason)
    }

    @Test
    fun suggest_floorsBaseBreakAtThreeMinutes() {
        val suggestion = AdaptiveBreakCalculator.suggest(
            baseBreakMinutes = 2,
            consecutiveSessionsToday = 0,
            currentHour = 9
        )

        assertEquals(3, suggestion.durationMinutes)
        assertEquals("Standard break", suggestion.reason)
    }

    @Test
    fun suggest_capsEveningBreakAndKeepsReason() {
        val suggestion = AdaptiveBreakCalculator.suggest(
            baseBreakMinutes = 20,
            consecutiveSessionsToday = 0,
            currentHour = 20
        )

        assertEquals(30, suggestion.durationMinutes)
        assertEquals("Evening wind-down", suggestion.reason)
    }

    @Test
    fun suggest_clampsOutOfRangeBaseForEveryBranch() {
        val belowMinimum = AdaptiveBreakCalculator.suggest(
            baseBreakMinutes = Int.MIN_VALUE,
            consecutiveSessionsToday = 0,
            currentHour = 10
        )
        val aboveMaximum = AdaptiveBreakCalculator.suggest(
            baseBreakMinutes = Int.MAX_VALUE,
            consecutiveSessionsToday = 0,
            currentHour = 9
        )

        assertEquals(3, belowMinimum.durationMinutes)
        assertEquals("Standard break", belowMinimum.reason)
        assertEquals(30, aboveMaximum.durationMinutes)
        assertEquals("Standard break", aboveMaximum.reason)
    }
}
