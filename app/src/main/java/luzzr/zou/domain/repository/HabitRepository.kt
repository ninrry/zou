package luzzr.zou.domain.repository

import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitDurationFinishResult
import luzzr.zou.domain.model.HabitStepAdvanceResult
import luzzr.zou.domain.model.HabitStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface HabitRepository {
    fun observeHabits(recordDate: Long, includeDeleted: Boolean): Flow<List<Habit>>

    fun observeDeletedHabits(recordDate: Long): Flow<List<Habit>> = emptyFlow()

    suspend fun getHabit(
        habitId: String,
        recordDate: Long,
        includeDeleted: Boolean = false,
    ): Habit?

    suspend fun saveHabit(habit: Habit, steps: List<HabitStep>)

    suspend fun softDeleteHabit(habitId: String)

    suspend fun restoreHabit(habitId: String)

    suspend fun hardDeleteHabit(habitId: String) = Unit

    suspend fun completeCheckHabit(habitId: String, recordDate: Long)

    suspend fun completeStepsHabit(habitId: String, recordDate: Long)

    suspend fun completeDurationHabit(
        habitId: String,
        recordDate: Long,
        durationMinutes: Int,
    )

    suspend fun advanceHabitStep(
        habitId: String,
        recordDate: Long,
    ): HabitStepAdvanceResult = HabitStepAdvanceResult()

    suspend fun revertHabitStep(
        habitId: String,
        recordDate: Long,
        stepId: String,
    ): HabitStepAdvanceResult = HabitStepAdvanceResult()

    suspend fun startHabitDuration(
        habitId: String,
        recordDate: Long,
    ) = Unit

    suspend fun pauseHabitDuration(
        habitId: String,
        recordDate: Long,
    ) = Unit

    suspend fun finishHabitDuration(
        habitId: String,
        recordDate: Long,
    ): HabitDurationFinishResult = HabitDurationFinishResult()

    suspend fun undoHabitCompletion(
        habitId: String,
        recordDate: Long,
    ) = Unit
}
