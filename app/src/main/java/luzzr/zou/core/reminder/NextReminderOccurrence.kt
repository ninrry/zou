package luzzr.zou.core.reminder

data class NextReminderOccurrence(
    val atMillis: Long,
    val reason: ReminderTriggerReason,
)
