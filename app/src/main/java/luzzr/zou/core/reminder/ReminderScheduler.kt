package luzzr.zou.core.reminder

interface ReminderScheduler {
    suspend fun scheduleTask(taskId: String)

    suspend fun cancelTask(taskId: String)

    suspend fun scheduleHabit(habitId: String)

    suspend fun cancelHabit(habitId: String)

    suspend fun rescheduleAllActiveReminders()
}
