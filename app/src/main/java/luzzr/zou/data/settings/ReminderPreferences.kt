package luzzr.zou.data.settings

data class ReminderPreferences(
    val defaultTaskRepeatIntervalMinutes: Int = 60,
    val defaultHabitRepeatIntervalMinutes: Int = 60,
    val showCompletedTasks: Boolean = false,
    val showOnlyTodayHabits: Boolean = false,
    val showDeletedHabits: Boolean = false,
    val settingsUpdatedAt: Long = 0L,
)
