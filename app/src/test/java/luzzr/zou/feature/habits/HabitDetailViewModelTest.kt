package luzzr.zou.feature.habits

import androidx.lifecycle.SavedStateHandle
import luzzr.zou.MainDispatcherRule
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitDurationFinishResult
import luzzr.zou.domain.model.HabitRecord
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.model.HabitStep
import luzzr.zou.domain.model.HabitStepAdvanceResult
import luzzr.zou.domain.repository.HabitRepository
import luzzr.zou.domain.usecase.AdvanceHabitStepUseCase
import luzzr.zou.domain.usecase.CheckInPolicy
import luzzr.zou.domain.usecase.CompleteCheckHabitUseCase
import luzzr.zou.domain.usecase.CompleteStepsHabitUseCase
import luzzr.zou.domain.usecase.FinishHabitDurationUseCase
import luzzr.zou.domain.usecase.GetHabitUseCase
import luzzr.zou.domain.usecase.HabitScheduleEvaluator
import luzzr.zou.domain.usecase.PauseHabitDurationUseCase
import luzzr.zou.domain.usecase.RevertHabitStepUseCase
import luzzr.zou.domain.usecase.SoftDeleteHabitUseCase
import luzzr.zou.domain.usecase.StartHabitDurationUseCase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun step_mode_should_persist_each_advance() = runTest {
        val date = LocalDate.of(2026, 3, 13)
        val repository = FakeHabitRepository(
            date = date,
            initialHabit = Habit(
                id = "habit-steps",
                title = "晚间整理",
                checkInMode = HabitCheckInMode.STEPS,
                steps = listOf(
                    HabitStep(id = "step-1", title = "收桌面", sortOrder = 0),
                    HabitStep(id = "step-2", title = "写复盘", sortOrder = 1),
                ),
            ),
        )
        val viewModel = createViewModel(repository, FixedTimeProvider(date))
        advanceUntilIdle()

        viewModel.onStepCheckedChanged(stepId = "step-1", checked = true)
        advanceUntilIdle()

        assertEquals(1, repository.advanceEvents.size)
        assertTrue(viewModel.uiState.value.steps.first { it.id == "step-1" }.checked)
    }

    @Test
    fun duration_mode_should_show_message_when_target_not_reached() = runTest {
        val date = LocalDate.of(2026, 3, 13)
        val repository = FakeHabitRepository(
            date = date,
            initialHabit = Habit(
                id = "habit-duration",
                title = "冥想",
                checkInMode = HabitCheckInMode.DURATION,
                targetDurationMinutes = 20,
            ),
        )
        val viewModel = createViewModel(repository, FixedTimeProvider(date))
        advanceUntilIdle()

        viewModel.onDurationFinish()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.actionMessage?.contains("未达标") == true)
    }

    private fun createViewModel(
        repository: FakeHabitRepository,
        timeProvider: TimeProvider,
    ): HabitDetailViewModel {
        return HabitDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(HabitRoutes.habitIdArg to repository.currentHabit.id)),
            getHabitUseCase = GetHabitUseCase(repository),
            completeCheckHabitUseCase = CompleteCheckHabitUseCase(repository),
            completeStepsHabitUseCase = CompleteStepsHabitUseCase(repository),
            advanceHabitStepUseCase = AdvanceHabitStepUseCase(repository),
            revertHabitStepUseCase = RevertHabitStepUseCase(repository),
            startHabitDurationUseCase = StartHabitDurationUseCase(repository),
            pauseHabitDurationUseCase = PauseHabitDurationUseCase(repository),
            finishHabitDurationUseCase = FinishHabitDurationUseCase(repository),
            softDeleteHabitUseCase = SoftDeleteHabitUseCase(repository),
            habitScheduleEvaluator = HabitScheduleEvaluator(),
            checkInPolicy = CheckInPolicy(timeProvider),
            timeProvider = timeProvider,
        )
    }

    private class FixedTimeProvider(
        private val date: LocalDate,
    ) : TimeProvider {
        override fun nowMillis(): Long = date
            .atTime(12, 0)
            .atZone(zoneId())
            .toInstant()
            .toEpochMilli()

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }

    private class FakeHabitRepository(
        private val date: LocalDate,
        initialHabit: Habit,
    ) : HabitRepository {
        private val habitFlow = MutableStateFlow(listOf(initialHabit))
        val advanceEvents = mutableListOf<String>()
        val currentHabit: Habit
            get() = habitFlow.value.first()

        override fun observeHabits(recordDate: Long, includeDeleted: Boolean): Flow<List<Habit>> = habitFlow

        override suspend fun getHabit(
            habitId: String,
            recordDate: Long,
            includeDeleted: Boolean,
        ): Habit? = habitFlow.value.firstOrNull { it.id == habitId }

        override suspend fun saveHabit(habit: Habit, steps: List<HabitStep>) = Unit

        override suspend fun softDeleteHabit(habitId: String) = Unit

        override suspend fun restoreHabit(habitId: String) = Unit

        override suspend fun completeCheckHabit(habitId: String, recordDate: Long) = Unit

        override suspend fun completeStepsHabit(habitId: String, recordDate: Long) {
            val habit = habitFlow.value.firstOrNull { it.id == habitId } ?: return
            val completedSteps = habit.steps.map { it.id }.toSet()
            habitFlow.value = habitFlow.value.map { current ->
                if (current.id == habitId) {
                    current.copy(
                        todayRecord = HabitRecord(
                            id = "record-$habitId",
                            habitId = habitId,
                            recordDate = recordDate,
                            status = HabitRecordStatus.COMPLETED,
                            stepProgressIds = completedSteps,
                        ),
                    )
                } else {
                    current
                }
            }
        }

        override suspend fun completeDurationHabit(habitId: String, recordDate: Long, durationMinutes: Int) = Unit

        override suspend fun advanceHabitStep(habitId: String, recordDate: Long): HabitStepAdvanceResult {
            val habit = habitFlow.value.firstOrNull { it.id == habitId } ?: return HabitStepAdvanceResult()
            val sortedStepIds = habit.steps.sortedBy { it.sortOrder }.map { it.id }
            val currentProgress = habit.todayRecord?.stepProgressIds.orEmpty().toMutableSet()
            val nextStepId = sortedStepIds.firstOrNull { it !in currentProgress }
                ?: return HabitStepAdvanceResult(
                    progressedStepId = null,
                    completedCount = currentProgress.size,
                    totalCount = sortedStepIds.size,
                    habitCompleted = habit.todayRecord?.status == HabitRecordStatus.COMPLETED,
                )
            currentProgress += nextStepId
            val completed = currentProgress.size == sortedStepIds.size
            advanceEvents += nextStepId
            habitFlow.value = habitFlow.value.map { current ->
                if (current.id == habitId) {
                    current.copy(
                        todayRecord = HabitRecord(
                            id = current.todayRecord?.id ?: "record-$habitId",
                            habitId = habitId,
                            recordDate = recordDate,
                            status = if (completed) HabitRecordStatus.COMPLETED else HabitRecordStatus.PENDING,
                            stepProgressIds = currentProgress,
                            createdAt = current.todayRecord?.createdAt ?: nowMillis(),
                            updatedAt = nowMillis(),
                        ),
                    )
                } else {
                    current
                }
            }
            return HabitStepAdvanceResult(
                progressedStepId = nextStepId,
                completedCount = currentProgress.size,
                totalCount = sortedStepIds.size,
                habitCompleted = completed,
            )
        }

        override suspend fun revertHabitStep(
            habitId: String,
            recordDate: Long,
            stepId: String,
        ): HabitStepAdvanceResult {
            val habit = habitFlow.value.firstOrNull { it.id == habitId } ?: return HabitStepAdvanceResult()
            val sortedStepIds = habit.steps.sortedBy { it.sortOrder }.map { it.id }
            val currentProgress = habit.todayRecord?.stepProgressIds.orEmpty().toMutableSet()
            if (!currentProgress.remove(stepId)) return HabitStepAdvanceResult()
            habitFlow.value = habitFlow.value.map { current ->
                if (current.id == habitId) {
                    current.copy(
                        todayRecord = HabitRecord(
                            id = current.todayRecord?.id ?: "record-$habitId",
                            habitId = habitId,
                            recordDate = recordDate,
                            status = HabitRecordStatus.PENDING,
                            stepProgressIds = currentProgress,
                            createdAt = current.todayRecord?.createdAt ?: nowMillis(),
                            updatedAt = nowMillis(),
                        ),
                    )
                } else {
                    current
                }
            }
            return HabitStepAdvanceResult(
                progressedStepId = stepId,
                completedCount = currentProgress.size,
                totalCount = sortedStepIds.size,
                habitCompleted = false,
            )
        }

        override suspend fun startHabitDuration(habitId: String, recordDate: Long) = Unit

        override suspend fun pauseHabitDuration(habitId: String, recordDate: Long) = Unit

        override suspend fun finishHabitDuration(habitId: String, recordDate: Long): HabitDurationFinishResult {
            return HabitDurationFinishResult(
                completed = false,
                elapsedSeconds = 10 * 60L,
                targetMinutes = 20,
            )
        }

        private fun nowMillis(): Long = date
            .atTime(12, 0)
            .atZone(ZoneId.of("Asia/Singapore"))
            .toInstant()
            .toEpochMilli()
    }
}
