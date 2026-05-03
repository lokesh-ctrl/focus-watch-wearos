package com.focusdial.app.data

object FocusScoreCalculator {

    fun calculate(
        plannedDurationMillis: Long,
        actualDurationMillis: Long,
        interruptionCount: Int,
        completed: Boolean
    ): Int {
        var score = 100

        val interruptionPenalty = (interruptionCount * 15).coerceAtMost(60)
        score -= interruptionPenalty

        if (!completed && plannedDurationMillis > 0) {
            val completionRatio = actualDurationMillis.toFloat() / plannedDurationMillis
            val remainingPercent = (1f - completionRatio).coerceIn(0f, 1f)
            score -= (remainingPercent * 40).toInt()
        }

        return score.coerceIn(0, 100)
    }
}
