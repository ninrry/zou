package luzzr.zou.core.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.dao.TaskDao
import luzzr.zou.domain.model.TaskStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val reminderScheduler: ReminderScheduler,
    private val reminderNotificationManager: ReminderNotificationManager,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(ReminderConstants.workerTaskIdKey)
            ?: return Result.failure()
        val task = taskDao.getActiveTaskEntity(taskId)

        if (task == null || task.isDeleted || task.status == TaskStatus.COMPLETED.name) {
            reminderScheduler.cancelTask(taskId)
            return Result.success()
        }
        if (task.dueAt != null && task.dueAt < timeProvider.nowMillis()) {
            reminderScheduler.cancelTask(taskId)
            return Result.success()
        }
        if (!notificationPermissionChecker.canPostNotifications()) {
            reminderScheduler.scheduleTask(taskId)
            return Result.success()
        }

        val reason = inputData.getString(ReminderConstants.workerTriggerReasonKey)
            ?.let { runCatching { ReminderTriggerReason.valueOf(it) }.getOrNull() }
            ?: ReminderTriggerReason.START
        val triggerAtMillis = inputData.getLong(ReminderConstants.workerTriggerAtKey, 0L)

        reminderNotificationManager.showTaskReminder(
            taskId = taskId,
            taskTitle = task.title,
            triggerAtMillis = triggerAtMillis,
            reason = reason,
            customTitle = task.reminderNotificationTitle,
            customBody = task.reminderNotificationBody,
        )
        reminderScheduler.scheduleTask(taskId)
        return Result.success()
    }
}
