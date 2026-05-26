package luzzr.zou.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.dao.HabitDao
import luzzr.zou.data.local.database.dao.TaskDao
import luzzr.zou.data.local.database.entity.HabitEntity
import luzzr.zou.data.local.database.entity.TaskEntity
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.model.TaskStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderNotificationManager: ReminderNotificationManager

    @Inject
    lateinit var notificationPermissionChecker: NotificationPermissionChecker

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var habitDao: HabitDao

    @Inject
    lateinit var taskReminderCalculator: TaskReminderCalculator

    @Inject
    lateinit var habitReminderCalculator: HabitReminderCalculator

    @Inject
    lateinit var habitReminderTimeCodec: HabitReminderTimeCodec

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE)?.let {
            runCatching { AlarmType.valueOf(it) }.getOrNull()
        } ?: return

        val pendingResult = goAsync()

        scope.launch {
            try {
                when (type) {
                    AlarmType.TASK -> handleTaskAlarm(intent)
                    AlarmType.HABIT -> handleHabitAlarm(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTaskAlarm(intent: Intent) {
        val taskId = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID) ?: return
        val triggerAtMillis = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, 0L)

        val task = taskDao.getActiveTaskEntity(taskId)
        if (task == null || task.isDeleted || task.status == TaskStatus.COMPLETED.name) {
            return
        }
        if (task.dueAt != null && task.dueAt < timeProvider.nowMillis()) {
            return
        }
        if (!notificationPermissionChecker.canPostNotifications()) {
            // 没有权限，只调度下一次，不发通知
            scheduleNextTask(taskId, task)
            return
        }

        reminderNotificationManager.showTaskReminder(
            taskId = taskId,
            taskTitle = task.title,
            triggerAtMillis = triggerAtMillis,
            reason = ReminderTriggerReason.START,
            customTitle = task.reminderNotificationTitle,
            customBody = task.reminderNotificationBody,
        )

        scheduleNextTask(taskId, task)
    }

    private suspend fun handleHabitAlarm(intent: Intent) {
        val habitId = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_ID) ?: return
        val triggerAtMillis = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, 0L)

        val habit = habitDao.getActiveHabitEntity(habitId)
        if (habit == null || habit.isDeleted) {
            return
        }

        val windowStart = habit.remindWindowStart?.let(LocalTime::parse)
        val windowEnd = habit.remindWindowEnd?.let(LocalTime::parse)
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

        if (occurrenceRecord?.status == HabitRecordStatus.COMPLETED.name) {
            scheduleNextHabit(habitId, habit)
            return
        }
        if (!notificationPermissionChecker.canPostNotifications()) {
            scheduleNextHabit(habitId, habit)
            return
        }

        reminderNotificationManager.showHabitReminder(
            habitId = habitId,
            habitTitle = habit.title,
            triggerAtMillis = triggerAtMillis,
            reason = ReminderTriggerReason.START,
            customTitle = habit.reminderNotificationTitle,
            customBody = habit.reminderNotificationBody,
        )

        scheduleNextHabit(habitId, habit)
    }

    private suspend fun scheduleNextTask(taskId: String, task: TaskEntity) {
        val now = timeProvider.nowMillis()
        val occurrence = taskReminderCalculator.calculateNext(
            spec = task.toReminderSpec(),
            nowMillis = now,
        ) ?: return

        alarmScheduler.scheduleAlarm(
            taskId = taskId,
            habitId = null,
            triggerAtMillis = occurrence.atMillis,
            type = AlarmType.TASK,
        )
    }

    private suspend fun scheduleNextHabit(habitId: String, habit: HabitEntity) {
        val now = timeProvider.nowMillis()
        val currentDate = timeProvider.currentDate()
        val completedEpochDays = buildList {
            add(currentDate)
            if (habit.hasCrossMidnightReminderWindow()) {
                add(currentDate.minusDays(1))
            }
        }
            .filter { date ->
                habitDao.getHabitRecordForDate(habitId, date.toEpochDay())?.status ==
                    HabitRecordStatus.COMPLETED.name
            }
            .mapTo(linkedSetOf()) { it.toEpochDay() }
        val spec = habit.toReminderSpec(completedEpochDays)
        val occurrence = habitReminderCalculator.calculateNext(spec, now) ?: return

        alarmScheduler.scheduleAlarm(
            taskId = habitId,
            habitId = habitId,
            triggerAtMillis = occurrence.atMillis,
            type = AlarmType.HABIT,
        )
    }

    private fun TaskEntity.toReminderSpec(): TaskReminderSpec {
        val zoneId = timeProvider.zoneId()
        return TaskReminderSpec(
            taskId = id,
            title = title,
            isCompleted = status == TaskStatus.COMPLETED.name,
            isDeleted = isDeleted,
            archived = archived,
            startReminderMinuteOfDay = startReminderMinuteOfDay,
            windowEndMinuteOfDay = windowEndMinuteOfDay,
            dueAt = dueAt,
            repeatIntervalMinutes = repeatIntervalMinutes,
            exactReminderTimes = emptyList(),
            allDay = allDay,
            reminderActiveFrom = startRemindAt?.let {
                java.time.Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
            },
            reminderActiveTo = remindWindowEndAt?.let {
                java.time.Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
            },
        )
    }

    private fun HabitEntity.toReminderSpec(completedEpochDays: Set<Long>): HabitReminderSpec {
        val frequencyValue = decodeFrequencyValue(frequencyValueJson)
        return HabitReminderSpec(
            habitId = id,
            title = title,
            frequencyType = frequencyType.toHabitFrequencyType(),
            selectedWeekdays = frequencyValue.weekdays
                .mapNotNull { value -> runCatching { DayOfWeek.of(value) }.getOrNull() }
                .toSet(),
            intervalDays = frequencyValue.intervalDays,
            intervalAnchorDate = frequencyValue.anchorDate?.let(java.time.LocalDate::parse),
            monthlyDays = frequencyValue.daysOfMonth.toSet(),
            remindWindowStart = remindWindowStart?.let(LocalTime::parse),
            remindWindowEnd = remindWindowEnd?.let(LocalTime::parse),
            repeatIntervalMinutes = repeatIntervalMinutes,
            exactReminderTimes = habitReminderTimeCodec.decode(exactReminderTimesJson),
            isDeleted = isDeleted,
            archived = archived,
            completedEpochDays = completedEpochDays,
        )
    }

    private fun HabitEntity.hasCrossMidnightReminderWindow(): Boolean {
        return reminderWindowCrossesMidnight(
            windowStart = remindWindowStart?.let(LocalTime::parse),
            windowEnd = remindWindowEnd?.let(LocalTime::parse),
        )
    }

    private fun decodeFrequencyValue(rawValue: String?): FrequencyValuePayload {
        if (rawValue.isNullOrBlank()) return FrequencyValuePayload()
        return runCatching {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            json.decodeFromString<FrequencyValuePayload>(rawValue)
        }.getOrDefault(FrequencyValuePayload())
    }

    private fun String.toHabitFrequencyType(): luzzr.zou.domain.model.HabitFrequencyType {
        return luzzr.zou.domain.model.HabitFrequencyType.entries.firstOrNull { it.name == this }
            ?: luzzr.zou.domain.model.HabitFrequencyType.DAILY
    }

    private companion object {
        const val TAG = "AlarmReminderReceiver"
    }

    @kotlinx.serialization.Serializable
    private data class FrequencyValuePayload(
        val weekdays: List<Int> = emptyList(),
        val intervalDays: Int? = null,
        val anchorDate: String? = null,
        val daysOfMonth: List<Int> = emptyList(),
    )
}
