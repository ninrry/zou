package luzzr.zou.core.reminder

import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.dao.HabitDao
import luzzr.zou.data.local.database.dao.TaskDao
import luzzr.zou.data.local.database.entity.HabitEntity
import luzzr.zou.data.local.database.entity.TaskEntity
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ReminderSchedulerImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val alarmScheduler: AlarmScheduler,
    private val taskReminderCalculator: TaskReminderCalculator,
    private val habitReminderCalculator: HabitReminderCalculator,
    private val reminderTimeCodec: ReminderTimeCodec,
    private val habitReminderTimeCodec: HabitReminderTimeCodec,
    private val timeProvider: TimeProvider,
) : ReminderScheduler {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun scheduleTask(taskId: String) {
        scheduleTaskInternal(taskId, taskDao.getActiveTaskEntity(taskId))
    }

    override suspend fun cancelTask(taskId: String) {
        alarmScheduler.cancelAlarm(taskId, null)
    }

    override suspend fun scheduleHabit(habitId: String) {
        scheduleHabitInternal(habitId, habitDao.getActiveHabitEntity(habitId))
    }

    override suspend fun cancelHabit(habitId: String) {
        alarmScheduler.cancelAlarm(habitId, habitId)
    }

    override suspend fun rescheduleAllActiveReminders() {
        taskDao.getActiveTaskEntities().forEach { task ->
            scheduleTaskInternal(task.id, task)
        }
        habitDao.getActiveHabitEntities().forEach { habit ->
            scheduleHabitInternal(habit.id, habit)
        }
    }

    private suspend fun scheduleTaskInternal(
        taskId: String,
        task: TaskEntity?,
    ) {
        if (
            task == null ||
            task.status == TaskStatus.COMPLETED.name ||
            task.isDeleted ||
            (task.dueAt != null && task.dueAt < timeProvider.nowMillis())
        ) {
            cancelTask(taskId)
            return
        }
        val now = timeProvider.nowMillis()
        val occurrence = taskReminderCalculator.calculateNext(
            spec = task.toReminderSpec(),
            nowMillis = now,
        ) ?: run {
            cancelTask(taskId)
            return
        }
        scheduleAlarm(taskId, null, occurrence.atMillis)
    }

    private suspend fun scheduleHabitInternal(
        habitId: String,
        habit: HabitEntity?,
    ) {
        if (habit == null || habit.isDeleted) {
            cancelHabit(habitId)
            return
        }
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
        val occurrence = habitReminderCalculator.calculateNext(spec, now) ?: run {
            cancelHabit(habitId)
            return
        }
        scheduleAlarm(habitId, habitId, occurrence.atMillis)
    }

    private fun scheduleAlarm(taskId: String, habitId: String?, triggerAtMillis: Long) {
        val type = if (habitId != null) AlarmType.HABIT else AlarmType.TASK
        alarmScheduler.scheduleAlarm(
            taskId = taskId,
            habitId = habitId,
            triggerAtMillis = triggerAtMillis,
            type = type,
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
            exactReminderTimes = reminderTimeCodec.decode(exactReminderTimesJson),
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
            intervalAnchorDate = frequencyValue.anchorDate?.let(LocalDate::parse),
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
        return runCatching { json.decodeFromString<FrequencyValuePayload>(rawValue) }
            .getOrDefault(FrequencyValuePayload())
    }

    private fun String.toHabitFrequencyType(): luzzr.zou.domain.model.HabitFrequencyType {
        return luzzr.zou.domain.model.HabitFrequencyType.entries.firstOrNull { it.name == this }
            ?: luzzr.zou.domain.model.HabitFrequencyType.DAILY
    }

    @Serializable
    private data class FrequencyValuePayload(
        val weekdays: List<Int> = emptyList(),
        val intervalDays: Int? = null,
        val anchorDate: String? = null,
        val daysOfMonth: List<Int> = emptyList(),
    )
}
