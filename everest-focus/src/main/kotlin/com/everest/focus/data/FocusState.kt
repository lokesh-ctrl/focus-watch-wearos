package com.everest.focus.data

sealed class FocusState {
    data object Idle : FocusState()

    data class Focus(
        val startTimeMillis: Long,
        val durationMillis: Long,
        val interruptionCount: Int = 0
    ) : FocusState()

    data class Break(
        val startTimeMillis: Long,
        val durationMillis: Long
    ) : FocusState()
}
