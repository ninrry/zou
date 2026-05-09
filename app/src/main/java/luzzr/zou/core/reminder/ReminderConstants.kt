package luzzr.zou.core.reminder

import android.app.NotificationManager

object ReminderConstants {
    // ── 通知频道 ──────────────────────────────────────────────
    const val channelTaskStart = "task_start"          // 任务开始提醒
    const val channelTaskDue = "task_due"              // 任务截止提醒
    const val channelHabitReminder = "habit_reminder"  // 习惯打卡提醒
    const val channelRepeat = "repeat_reminder"        // 重复提醒

    val channelConfigs = listOf(
        ChannelConfig(channelTaskStart, "任务开始", NotificationManager.IMPORTANCE_HIGH),
        ChannelConfig(channelTaskDue, "任务截止", NotificationManager.IMPORTANCE_HIGH),
        ChannelConfig(channelHabitReminder, "习惯提醒", NotificationManager.IMPORTANCE_HIGH),
        ChannelConfig(channelRepeat, "重复提醒", NotificationManager.IMPORTANCE_DEFAULT),
    )

    data class ChannelConfig(
        val id: String,
        val name: String,
        val importance: Int,
    )

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
