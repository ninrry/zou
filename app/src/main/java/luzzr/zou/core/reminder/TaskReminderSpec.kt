package luzzr.zou.core.reminder

data class TaskReminderSpec(
    val taskId: String,
    val title: String,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
    val archived: Boolean,
    val startReminderMinuteOfDay: Int?,
    val windowEndMinuteOfDay: Int?,
    val dueAt: Long?,
    val repeatIntervalMinutes: Int?,
    val exactReminderTimes: List<Long>,
    val allDay: Boolean,
) {
    val hasReminderConfiguration: Boolean
        get() = startReminderMinuteOfDay != null ||
            repeatIntervalMinutes != null ||
            exactReminderTimes.isNotEmpty()
}
