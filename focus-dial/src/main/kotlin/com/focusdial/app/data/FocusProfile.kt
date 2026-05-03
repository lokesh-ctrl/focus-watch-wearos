package com.focusdial.app.data

data class FocusProfile(
    val id: String,
    val name: String,
    val focusMinutes: Int,
    val breakMinutes: Int
) {
    companion object {
        val DEEP_WORK = FocusProfile("deep_work", "Deep Work", 50, 10)
        val STUDY = FocusProfile("study", "Study", 25, 5)
        val SPRINT = FocusProfile("sprint", "Sprint", 15, 3)

        val ALL = listOf(DEEP_WORK, STUDY, SPRINT)

        fun fromId(id: String): FocusProfile? = ALL.find { it.id == id }
    }
}
