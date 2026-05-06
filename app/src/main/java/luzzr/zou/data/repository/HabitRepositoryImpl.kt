package luzzr.zou.data.repository

import luzzr.zou.core.reminder.HabitReminderTimeCodec
import luzzr.zou.core.reminder.ReminderDispatchQueue
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.dao.HabitDao
import luzzr.zou.data.local.database.entity.HabitEntity
import luzzr.zou.data.local.database.entity.HabitRecordEntity
import luzzr.zou.data.local.database.entity.HabitStepEntity
import luzzr.zou.data.local.database.model.HabitWithSteps
import luzzr.zou.domain.model.HabitDurationFinishResult
import luzzr.zou.domain.model.HabitStepAdvanceResult
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitRecord
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.model.HabitStep
import luzzr.zou.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val reminderDispatchQueue: ReminderDispatchQueue,
    private val habitReminderTimeCodec: HabitReminderTimeCodec,
    private val timeProvider: TimeProvider,
) : HabitRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeHabits(
        recordDate: Long,
        includeDeleted: Boolean,
    ): Flow<List<Habit>> {
        return combine(
            habitDao.observeHabitsWithSteps(includeDeleted),
            habitDao.observeRecordsForDate(recordDate),
        ) { habits, records ->
            val recordsByHabitId = records.associateBy { it.habitId }
            habits.map { habitWithSteps ->
                mapHabit(
                    habitWithSteps = habitWithSteps,
                    todayRecord = recordsByHabitId[habitWithSteps.habit.id],
                )
            }
        }
    }

    override fun observeDeletedHabits(recordDate: Long): Flow<List<Habit>> {
        return observeHabits(
            recordDate = recordDate,
            includeDeleted = true,
        ).map { habits ->
            habits.filter { it.isDeleted }
        }
    }

    override suspend fun getHabit(
        habitId: String,
        recordDate: Long,
        includeDeleted: Boolean,
    ): Habit? {
        val habitWithSteps = habitDao.getHabitWithSteps(
            habitId = habitId,
            includeDeleted = includeDeleted,
        ) ?: return null
        return mapHabit(
            habitWithSteps = habitWithSteps,
            todayRecord = habitDao.getHabitRecordForDate(habitId, recordDate),
        )
    }

    override suspend fun saveHabit(habit: Habit, steps: List<HabitStep>) {
        val now = timeProvider.nowMillis()
        val cleanedSteps = steps
            .filter { it.title.isNotBlank() }
            .mapIndexed { index, step ->
                step.copy(
                    sortOrder = index,
                    createdAt = if (step.createdAt == 0L) now else step.createdAt,
                    updatedAt = now,
                )
            }
        val normalizedHabit = habit.copy(
            createdAt = if (habit.createdAt == 0L) now else habit.createdAt,
            updatedAt = now,
        )
        habitDao.saveHabitWithSteps(
            habit = normalizedHabit.toEntity(),
            steps = cleanedSteps.map { it.toEntity(normalizedHabit.id) },
            updatedAt = now,
        )
        reminderDispatchQueue.scheduleHabit(normalizedHabit.id)
    }

    override suspend fun softDeleteHabit(habitId: String) {
        habitDao.softDeleteHabit(
            habitId = habitId,
            deletedAt = timeProvider.nowMillis(),
        )
        reminderDispatchQueue.cancelHabit(habitId)
    }

    override suspend fun restoreHabit(habitId: String) {
        habitDao.restoreHabit(
            habitId = habitId,
            updatedAt = timeProvider.nowMillis(),
        )
        reminderDispatchQueue.scheduleHabit(habitId)
    }

    override suspend fun hardDeleteHabit(habitId: String) {
        habitDao.hardDeleteHabitTree(habitId)
        reminderDispatchQueue.cancelHabit(habitId)
    }

    override suspend fun completeCheckHabit(habitId: String, recordDate: Long) {
        upsertCompletedRecord(
            habitId = habitId,
            recordDate = recordDate,
            durationMinutes = null,
            stepProgressIds = emptySet(),
            durationElapsedSeconds = 0L,
            durationRunningSinceMillis = null,
        )
    }

    override suspend fun completeStepsHabit(habitId: String, recordDate: Long) {
        val habit = habitDao.getHabitWithSteps(habitId, includeDeleted = false) ?: return
        val stepIds = habit.steps.filterNot { it.isDeleted }.map { it.id }.toSet()
        upsertCompletedRecord(
            habitId = habitId,
            recordDate = recordDate,
            durationMinutes = null,
            stepProgressIds = stepIds,
            durationElapsedSeconds = 0L,
            durationRunningSinceMillis = null,
        )
    }

    override suspend fun completeDurationHabit(
        habitId: String,
        recordDate: Long,
        durationMinutes: Int,
    ) {
        val elapsedSeconds = durationMinutes.toLong().coerceAtLeast(0L) * 60L
        upsertCompletedRecord(
            habitId = habitId,
            recordDate = recordDate,
            durationMinutes = durationMinutes,
            stepProgressIds = emptySet(),
            durationElapsedSeconds = elapsedSeconds,
            durationRunningSinceMillis = null,
        )
    }

    override suspend fun advanceHabitStep(
        habitId: String,
        recordDate: Long,
    ): HabitStepAdvanceResult {
        val habit = habitDao.getHabitWithSteps(habitId, includeDeleted = false) ?: return HabitStepAdvanceResult()
        val stepIds = habit.steps
            .filterNot { it.isDeleted }
            .sortedBy { it.sortOrder }
            .map { it.id }
        if (stepIds.isEmpty()) return HabitStepAdvanceResult()

        val now = timeProvider.nowMillis()
        val existingRecord = habitDao.getHabitRecordForDate(habitId, recordDate)
        val progress = decodeStepProgress(existingRecord?.stepProgressJson)
            .filter { it in stepIds }
            .toMutableSet()
        val nextStepId = stepIds.firstOrNull { it !in progress }
            ?: return HabitStepAdvanceResult(
                progressedStepId = null,
                completedCount = progress.size,
                totalCount = stepIds.size,
                habitCompleted = existingRecord?.status == HabitRecordStatus.COMPLETED.name,
            )

        progress += nextStepId
        val completed = progress.size >= stepIds.size
        val updatedRecord = HabitRecordEntity(
            id = existingRecord?.id ?: UUID.randomUUID().toString(),
            habitId = habitId,
            recordDate = recordDate,
            status = if (completed) HabitRecordStatus.COMPLETED.name else HabitRecordStatus.PENDING.name,
            durationMinutes = existingRecord?.durationMinutes,
            stepProgressJson = encodeStepProgress(progress),
            durationElapsedSeconds = existingRecord?.durationElapsedSeconds ?: 0L,
            durationRunningSinceMillis = existingRecord?.durationRunningSinceMillis,
            createdAt = existingRecord?.createdAt ?: now,
            updatedAt = now,
        )
        habitDao.upsertHabitRecord(updatedRecord)
        reminderDispatchQueue.scheduleHabit(habitId)
        return HabitStepAdvanceResult(
            progressedStepId = nextStepId,
            completedCount = progress.size,
            totalCount = stepIds.size,
            habitCompleted = completed,
        )
    }

    override suspend fun revertHabitStep(
        habitId: String,
        recordDate: Long,
        stepId: String,
    ): HabitStepAdvanceResult {
        val habit = habitDao.getHabitWithSteps(habitId, includeDeleted = false) ?: return HabitStepAdvanceResult()
        val stepIds = habit.steps
            .filterNot { it.isDeleted }
            .sortedBy { it.sortOrder }
            .map { it.id }
        if (stepIds.isEmpty()) return HabitStepAdvanceResult()

        val now = timeProvider.nowMillis()
        val existingRecord = habitDao.getHabitRecordForDate(habitId, recordDate) ?: return HabitStepAdvanceResult()
        val progress = decodeStepProgress(existingRecord.stepProgressJson)
            .filter { it in stepIds }
            .toMutableSet()
        if (stepId !in progress) {
            return HabitStepAdvanceResult(
                progressedStepId = null,
                completedCount = progress.size,
                totalCount = stepIds.size,
                habitCompleted = existingRecord.status == HabitRecordStatus.COMPLETED.name,
            )
        }

        progress -= stepId
        val updatedRecord = existingRecord.copy(
            status = HabitRecordStatus.PENDING.name,
            stepProgressJson = encodeStepProgress(progress),
            durationRunningSinceMillis = null,
            updatedAt = now,
        )
        habitDao.upsertHabitRecord(updatedRecord)
        reminderDispatchQueue.scheduleHabit(habitId)
        return HabitStepAdvanceResult(
            progressedStepId = stepId,
            completedCount = progress.size,
            totalCount = stepIds.size,
            habitCompleted = false,
        )
    }

    override suspend fun startHabitDuration(habitId: String, recordDate: Long) {
        val habit = habitDao.getActiveHabitEntity(habitId) ?: return
        if (habit.checkInMode != HabitCheckInMode.DURATION.name) return

        val now = timeProvider.nowMillis()
        val existingRecord = habitDao.getHabitRecordForDate(habitId, recordDate)
        if (existingRecord?.status == HabitRecordStatus.COMPLETED.name) return
        if (existingRecord?.durationRunningSinceMillis != null) return

        val nextRecord = HabitRecordEntity(
            id = existingRecord?.id ?: UUID.randomUUID().toString(),
            habitId = habitId,
            recordDate = recordDate,
            status = existingRecord?.status ?: HabitRecordStatus.PENDING.name,
            durationMinutes = existingRecord?.durationMinutes,
            stepProgressJson = existingRecord?.stepProgressJson,
            durationElapsedSeconds = existingRecord?.durationElapsedSeconds ?: 0L,
            durationRunningSinceMillis = now,
            createdAt = existingRecord?.createdAt ?: now,
            updatedAt = now,
        )
        habitDao.upsertHabitRecord(nextRecord)
        reminderDispatchQueue.scheduleHabit(habitId)
    }

    override suspend fun pauseHabitDuration(habitId: String, recordDate: Long) {
        val now = timeProvider.nowMillis()
        val existingRecord = habitDao.getHabitRecordForDate(habitId, recordDate) ?: return
        val runningSince = existingRecord.durationRunningSinceMillis ?: return
        val elapsedSeconds = existingRecord.durationElapsedSeconds +
            ((now - runningSince) / 1_000L).coerceAtLeast(0L)
        habitDao.upsertHabitRecord(
            existingRecord.copy(
                durationElapsedSeconds = elapsedSeconds,
                durationRunningSinceMillis = null,
                updatedAt = now,
            ),
        )
        reminderDispatchQueue.scheduleHabit(habitId)
    }

    override suspend fun finishHabitDuration(
        habitId: String,
        recordDate: Long,
    ): HabitDurationFinishResult {
        val habit = habitDao.getHabitWithSteps(habitId, includeDeleted = false)?.habit
            ?: return HabitDurationFinishResult()
        if (habit.checkInMode != HabitCheckInMode.DURATION.name) return HabitDurationFinishResult()

        val now = timeProvider.nowMillis()
        val existingRecord = habitDao.getHabitRecordForDate(habitId, recordDate)
        val runningSince = existingRecord?.durationRunningSinceMillis
        val elapsedSeconds = (existingRecord?.durationElapsedSeconds ?: 0L) +
            if (runningSince == null) 0L else ((now - runningSince) / 1_000L).coerceAtLeast(0L)
        val targetMinutes = habit.targetDurationMinutes
        val reached = when {
            targetMinutes == null || targetMinutes <= 0 -> elapsedSeconds > 0L
            else -> elapsedSeconds >= targetMinutes * 60L
        }
        if (!reached) {
            return HabitDurationFinishResult(
                completed = false,
                elapsedSeconds = elapsedSeconds,
                targetMinutes = targetMinutes,
            )
        }

        val durationMinutes = ((elapsedSeconds + 59L) / 60L).toInt()
        val nextRecord = HabitRecordEntity(
            id = existingRecord?.id ?: UUID.randomUUID().toString(),
            habitId = habitId,
            recordDate = recordDate,
            status = HabitRecordStatus.COMPLETED.name,
            durationMinutes = durationMinutes,
            stepProgressJson = existingRecord?.stepProgressJson,
            durationElapsedSeconds = elapsedSeconds,
            durationRunningSinceMillis = null,
            createdAt = existingRecord?.createdAt ?: now,
            updatedAt = now,
        )
        habitDao.upsertHabitRecord(nextRecord)
        reminderDispatchQueue.scheduleHabit(habitId)
        return HabitDurationFinishResult(
            completed = true,
            elapsedSeconds = elapsedSeconds,
            targetMinutes = targetMinutes,
        )
    }

    override suspend fun undoHabitCompletion(habitId: String, recordDate: Long) {
        val existingRecord = habitDao.getHabitRecordForDate(habitId, recordDate) ?: return
        val now = timeProvider.nowMillis()
        habitDao.upsertHabitRecord(
            existingRecord.copy(
                status = HabitRecordStatus.PENDING.name,
                durationMinutes = null,
                durationRunningSinceMillis = null,
                updatedAt = now,
            ),
        )
        reminderDispatchQueue.scheduleHabit(habitId)
    }

    private suspend fun upsertCompletedRecord(
        habitId: String,
        recordDate: Long,
        durationMinutes: Int?,
        stepProgressIds: Set<String>,
        durationElapsedSeconds: Long,
        durationRunningSinceMillis: Long?,
    ) {
        val now = timeProvider.nowMillis()
        val existingRecord = habitDao.getHabitRecordForDate(habitId, recordDate)
        habitDao.upsertHabitRecord(
            HabitRecordEntity(
                id = existingRecord?.id ?: UUID.randomUUID().toString(),
                habitId = habitId,
                recordDate = recordDate,
                status = HabitRecordStatus.COMPLETED.name,
                durationMinutes = durationMinutes,
                stepProgressJson = encodeStepProgress(stepProgressIds),
                durationElapsedSeconds = durationElapsedSeconds,
                durationRunningSinceMillis = durationRunningSinceMillis,
                createdAt = existingRecord?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        reminderDispatchQueue.scheduleHabit(habitId)
    }

    private fun mapHabit(
        habitWithSteps: HabitWithSteps,
        todayRecord: HabitRecordEntity?,
    ): Habit {
        val frequencyValue = decodeFrequencyValue(habitWithSteps.habit.frequencyValueJson)
        return Habit(
            id = habitWithSteps.habit.id,
            title = habitWithSteps.habit.title,
            contentMarkdown = habitWithSteps.habit.contentMarkdown,
            frequencyType = habitWithSteps.habit.frequencyType.toHabitFrequencyType(),
            selectedWeekdays = frequencyValue.weekdays
                .mapNotNull { value -> runCatching { DayOfWeek.of(value) }.getOrNull() }
                .toSet(),
            intervalDays = frequencyValue.intervalDays,
            intervalAnchorDate = frequencyValue.anchorDate?.let(LocalDate::parse),
            monthlyDays = frequencyValue.daysOfMonth.toSet(),
            remindWindowStart = habitWithSteps.habit.remindWindowStart?.let(LocalTime::parse),
            remindWindowEnd = habitWithSteps.habit.remindWindowEnd?.let(LocalTime::parse),
            repeatIntervalMinutes = habitWithSteps.habit.repeatIntervalMinutes,
            exactReminderTimes = habitReminderTimeCodec.decode(habitWithSteps.habit.exactReminderTimesJson),
            checkInMode = habitWithSteps.habit.checkInMode.toHabitCheckInMode(),
            targetDurationMinutes = habitWithSteps.habit.targetDurationMinutes,
            streakCountCache = habitWithSteps.habit.streakCountCache,
            createdAt = habitWithSteps.habit.createdAt,
            updatedAt = habitWithSteps.habit.updatedAt,
            isDeleted = habitWithSteps.habit.isDeleted,
            deletedAt = habitWithSteps.habit.deletedAt,
            tags = habitWithSteps.habit.tags,
            archived = habitWithSteps.habit.archived,
            steps = habitWithSteps.steps
                .filterNot { it.isDeleted }
                .sortedBy { it.sortOrder }
                .map { step ->
                    HabitStep(
                        id = step.id,
                        title = step.title,
                        sortOrder = step.sortOrder,
                        createdAt = step.createdAt,
                        updatedAt = step.updatedAt,
                    )
                },
            todayRecord = todayRecord?.toDomain(),
            reminderNotificationTitle = habitWithSteps.habit.reminderNotificationTitle,
            reminderNotificationBody = habitWithSteps.habit.reminderNotificationBody,
        )
    }

    private fun Habit.toEntity(): HabitEntity {
        return HabitEntity(
            id = id,
            title = title,
            contentMarkdown = contentMarkdown,
            frequencyType = frequencyType.name,
            frequencyValueJson = encodeFrequencyValue(
                FrequencyValuePayload(
                    weekdays = selectedWeekdays.map { it.value }.sorted(),
                    intervalDays = intervalDays,
                    anchorDate = intervalAnchorDate?.toString(),
                    daysOfMonth = monthlyDays.sorted(),
                ),
            ),
            remindWindowStart = remindWindowStart?.toString(),
            remindWindowEnd = remindWindowEnd?.toString(),
            repeatIntervalMinutes = repeatIntervalMinutes,
            exactReminderTimesJson = habitReminderTimeCodec.encode(exactReminderTimes),
            checkInMode = checkInMode.name,
            targetDurationMinutes = targetDurationMinutes,
            streakCountCache = streakCountCache,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            tags = tags,
            archived = archived,
            reminderNotificationTitle = reminderNotificationTitle,
            reminderNotificationBody = reminderNotificationBody,
        )
    }

    private fun HabitStep.toEntity(habitId: String): HabitStepEntity {
        return HabitStepEntity(
            id = id,
            habitId = habitId,
            title = title,
            sortOrder = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = false,
        )
    }

    private fun HabitRecordEntity.toDomain(): HabitRecord {
        return HabitRecord(
            id = id,
            habitId = habitId,
            recordDate = recordDate,
            status = status.toHabitRecordStatus(),
            durationMinutes = durationMinutes,
            stepProgressIds = decodeStepProgress(stepProgressJson),
            durationElapsedSeconds = durationElapsedSeconds,
            durationRunningSinceMillis = durationRunningSinceMillis,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun decodeFrequencyValue(rawValue: String?): FrequencyValuePayload {
        if (rawValue.isNullOrBlank()) return FrequencyValuePayload()
        return runCatching { json.decodeFromString<FrequencyValuePayload>(rawValue) }
            .getOrDefault(FrequencyValuePayload())
    }

    private fun encodeFrequencyValue(payload: FrequencyValuePayload): String? {
        if (
            payload.weekdays.isEmpty() &&
            payload.intervalDays == null &&
            payload.anchorDate == null &&
            payload.daysOfMonth.isEmpty()
        ) {
            return null
        }
        return json.encodeToString(payload)
    }

    private fun decodeStepProgress(rawValue: String?): Set<String> {
        if (rawValue.isNullOrBlank()) return emptySet()
        return runCatching { json.decodeFromString<List<String>>(rawValue) }
            .getOrDefault(emptyList())
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun encodeStepProgress(stepIds: Set<String>): String? {
        if (stepIds.isEmpty()) return null
        return json.encodeToString(stepIds.sorted())
    }

    private fun String.toHabitFrequencyType(): HabitFrequencyType {
        return HabitFrequencyType.entries.firstOrNull { it.name == this }
            ?: HabitFrequencyType.DAILY
    }

    private fun String.toHabitCheckInMode(): HabitCheckInMode {
        return HabitCheckInMode.entries.firstOrNull { it.name == this }
            ?: HabitCheckInMode.CHECK
    }

    private fun String.toHabitRecordStatus(): HabitRecordStatus {
        return HabitRecordStatus.entries.firstOrNull { it.name == this }
            ?: HabitRecordStatus.PENDING
    }

    @Serializable
    private data class FrequencyValuePayload(
        val weekdays: List<Int> = emptyList(),
        val intervalDays: Int? = null,
        val anchorDate: String? = null,
        val daysOfMonth: List<Int> = emptyList(),
    )
}
