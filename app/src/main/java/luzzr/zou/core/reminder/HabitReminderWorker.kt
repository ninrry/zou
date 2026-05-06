package luzzr.zou.core.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.dao.HabitDao
import luzzr.zou.domain.model.HabitRecordStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalTime

@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitDao: HabitDao,
    private val reminderScheduler: ReminderScheduler,
    private val reminderNotificationManager: ReminderNotificationManager,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(ReminderConstants.workerHabitIdKey)
            ?: return Result.failure()
        val habit = habitDao.getActiveHabitEntity(habitId)
        val reason = inputData.getString(ReminderConstants.workerTriggerReasonKey)
            ?.let { runCatching { ReminderTriggerReason.valueOf(it) }.getOrNull() }
            ?: ReminderTriggerReason.START
        val triggerAtMillis = inputData.getLong(ReminderConstants.workerTriggerAtKey, 0L)
        val windowStart = habit?.remindWindowStart?.let(LocalTime::parse)
        val windowEnd = habit?.remindWindowEnd?.let(LocalTime::parse)
        val occurrenceDate = if (triggerAtMillis > 0L) {
            habitReminderOccurrenceBaseDate(
                occurrenceMillis = triggerAtMillis,
                zoneId = timeProvider.zoneId(),
                windowStart = windowStart,
                windowEnd = windowEnd,
            )
        } else {
            timeProvider.currentDate()
        }
        val occurrenceRecord = habitDao.getHabitRecordForDate(habitId, occurrenceDate.toEpochDay())

        if (habit == null || habit.isDeleted) {
            reminderScheduler.cancelHabit(habitId)
            return Result.success()
        }
        if (occurrenceRecord?.status == HabitRecordStatus.COMPLETED.name) {
            reminderScheduler.scheduleHabit(habitId)
            return Result.success()
        }
        if (!notificationPermissionChecker.canPostNotifications()) {
            reminderScheduler.scheduleHabit(habitId)
            return Result.success()
        }
        if (
            reason != ReminderTriggerReason.EXACT &&
            triggerAtMillis > 0L &&
            windowEnd != null &&
            timeProvider.nowMillis() > habitReminderBoundaryMillis(
                occurrenceMillis = triggerAtMillis,
                zoneId = timeProvider.zoneId(),
                windowStart = windowStart,
                windowEnd = windowEnd,
            )
        ) {
            reminderScheduler.scheduleHabit(habitId)
            return Result.success()
        }

        reminderNotificationManager.showHabitReminder(
            habitId = habitId,
            habitTitle = habit.title,
            triggerAtMillis = triggerAtMillis,
            reason = reason,
            customTitle = habit.reminderNotificationTitle,
            customBody = habit.reminderNotificationBody,
        )
        reminderScheduler.scheduleHabit(habitId)
        return Result.success()
    }
}
