package com.focusdial.app.data

sealed class FocusState {
    data object Idle : FocusState()

    data class Focus(
        val startTimeMillis: Long,
        val durationMillis: Long,
        val interruptionCount: Int = 0
    ) : FocusState()

    data class Paused(
        val originalStartTimeMillis: Long,
        val durationMillis: Long,
        val elapsedBeforePauseMillis: Long,
        val pausedAtMillis: Long,
        val interruptionCount: Int = 0
    ) : FocusState()

    data class Break(
        val startTimeMillis: Long,
        val durationMillis: Long
    ) : FocusState()
}
