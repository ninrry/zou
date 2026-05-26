package luzzr.zou.core.reminder

interface ReminderDispatchQueue {
    fun scheduleTask(taskId: String)
    fun cancelTask(taskId: String)
    fun scheduleHabit(habitId: String)
    fun cancelHabit(habitId: String)
    fun rescheduleAllActiveReminders()
}
