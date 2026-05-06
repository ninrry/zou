package luzzr.zou.core.reminder

object ReminderConstants {
    const val notificationChannelId = "task_reminders"
    const val notificationChannelName = "提醒"
    const val alertTitleExtra = "reminder_alert_title"
    const val alertBodyExtra = "reminder_alert_body"
    const val alertNotificationIdExtra = "reminder_alert_notification_id"
    const val alertIsHabitExtra = "reminder_alert_is_habit"
    const val taskIdExtra = "task_id"
    const val habitIdExtra = "habit_id"
    const val workerTaskIdKey = "worker_task_id"
    const val workerHabitIdKey = "worker_habit_id"
    const val workerTriggerAtKey = "worker_trigger_at"
    const val workerTriggerReasonKey = "worker_trigger_reason"
    const val reminderRecoveryWorkName = "reminder_recovery_now"
    const val reminderHealthCheckWorkName = "reminder_health_check"

    fun uniqueWorkName(taskId: String): String = "task_reminder_$taskId"

    fun uniqueHabitWorkName(habitId: String): String = "habit_reminder_$habitId"

    fun notificationId(taskId: String): Int = taskId.hashCode()

    fun habitNotificationId(habitId: String): Int = habitId.hashCode()
}
