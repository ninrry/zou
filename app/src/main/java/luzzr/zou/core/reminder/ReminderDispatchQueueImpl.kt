package luzzr.zou.core.reminder

import android.util.Log
import luzzr.zou.di.ApplicationScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Singleton
class ReminderDispatchQueueImpl @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val reminderScheduler: ReminderScheduler,
) : ReminderDispatchQueue {

    private val jobs = ConcurrentHashMap<String, Job>()

    override fun scheduleTask(taskId: String) {
        enqueue(resourceKey = "task:$taskId", operation = "scheduleTask") {
            reminderScheduler.scheduleTask(taskId)
        }
    }

    override fun cancelTask(taskId: String) {
        enqueue(resourceKey = "task:$taskId", operation = "cancelTask") {
            reminderScheduler.cancelTask(taskId)
        }
    }

    override fun scheduleHabit(habitId: String) {
        enqueue(resourceKey = "habit:$habitId", operation = "scheduleHabit") {
            reminderScheduler.scheduleHabit(habitId)
        }
    }

    override fun cancelHabit(habitId: String) {
        enqueue(resourceKey = "habit:$habitId", operation = "cancelHabit") {
            reminderScheduler.cancelHabit(habitId)
        }
    }

    override fun rescheduleAllActiveReminders() {
        enqueue(resourceKey = "all-reminders", operation = "rescheduleAllActiveReminders") {
            reminderScheduler.rescheduleAllActiveReminders()
        }
    }

    private fun enqueue(
        resourceKey: String,
        operation: String,
        block: suspend () -> Unit,
    ) {
        jobs.remove(resourceKey)?.cancel()
        val job = applicationScope.launch {
            runCatching { block() }
                .onFailure { throwable ->
                    Log.e(TAG, "Reminder dispatch failed: $operation ($resourceKey)", throwable)
                }
            jobs.remove(resourceKey)
        }
        jobs[resourceKey] = job
    }

    private companion object {
        const val TAG = "ReminderDispatchQueue"
    }
}
