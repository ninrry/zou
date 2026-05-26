package luzzr.zou.feature.habits

import luzzr.zou.MainDispatcherRule
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitRecord
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.repository.HabitRepository
import luzzr.zou.domain.repository.SettingsRepository
import luzzr.zou.domain.usecase.CompleteCheckHabitUseCase
import luzzr.zou.domain.usecase.CheckInPolicy
import luzzr.zou.domain.usecase.HabitScheduleEvaluator
import luzzr.zou.domain.usecase.ObserveReminderPreferencesUseCase
import luzzr.zou.domain.usecase.ObserveHabitsUseCase
import luzzr.zou.domain.usecase.RestoreHabitUseCase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun todayOnlyFiltersHabitsAndDeletedListCanBeShown() = runTest {
        val repository = FakeHabitRepository()
        val settingsRepository = FakeSettingsRepository()
        repository.habits.value = listOf(
            habit(id = "daily", title = "Daily", frequencyType = HabitFrequencyType.DAILY),
            habit(
                id = "weekly",
                title = "Weekly",
                frequencyType = HabitFrequencyType.WEEKLY,
                selectedWeekdays = setOf(DayOfWeek.TUESDAY),
            ),
            habit(
                id = "deleted",
                title = "Deleted",
                frequencyType = HabitFrequencyType.DAILY,
                isDeleted = true,
            ),
        )

        val viewModel = HabitsViewModel(
            observeReminderPreferencesUseCase = ObserveReminderPreferencesUseCase(settingsRepository),
            observeHabitsUseCase = ObserveHabitsUseCase(repository),
            completeCheckHabitUseCase = CompleteCheckHabitUseCase(repository),
            restoreHabitUseCase = RestoreHabitUseCase(repository),
            habitScheduleEvaluator = HabitScheduleEvaluator(),
            checkInPolicy = CheckInPolicy(FixedTimeProvider()),
            timeProvider = FixedTimeProvider(),
        )
        val job = backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()
        assertEquals(listOf("Daily", "Weekly"), viewModel.uiState.value.activeHabits.map { it.title })

        settingsRepository.state.value = settingsRepository.state.value.copy(showOnlyTodayHabits = true)
        advanceUntilIdle()

        assertEquals(listOf("Daily"), viewModel.uiState.value.activeHabits.map { it.title })

        settingsRepository.state.value = settingsRepository.state.value.copy(showDeletedHabits = true)
        advanceUntilIdle()

        assertEquals(listOf("Deleted"), viewModel.uiState.value.deletedHabits.map { it.title })

        job.cancel()
    }

    @Test
    fun quickCheckMarksCheckHabitAsCompleted() = runTest {
        val repository = FakeHabitRepository()
        val settingsRepository = FakeSettingsRepository()
        repository.habits.value = listOf(
            habit(id = "habit-1", title = "Walk", frequencyType = HabitFrequencyType.DAILY),
        )
        val viewModel = HabitsViewModel(
            observeReminderPreferencesUseCase = ObserveReminderPreferencesUseCase(settingsRepository),
            observeHabitsUseCase = ObserveHabitsUseCase(repository),
            completeCheckHabitUseCase = CompleteCheckHabitUseCase(repository),
            restoreHabitUseCase = RestoreHabitUseCase(repository),
            habitScheduleEvaluator = HabitScheduleEvaluator(),
            checkInPolicy = CheckInPolicy(FixedTimeProvider()),
            timeProvider = FixedTimeProvider(),
        )
        val job = backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()
        viewModel.onQuickCheckHabit("habit-1")
        advanceUntilIdle()

        assertEquals("今日已完成", viewModel.uiState.value.activeHabits.single().statusText)

        job.cancel()
    }

    private class FixedTimeProvider : TimeProvider {
        override fun nowMillis(): Long = LocalDate.of(2026, 3, 9)
            .atStartOfDay(ZoneId.of("Asia/Singapore"))
            .toInstant()
            .toEpochMilli()

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }

    private class FakeHabitRepository : HabitRepository {
        val habits = MutableStateFlow<List<Habit>>(emptyList())

        override fun observeHabits(recordDate: Long, includeDeleted: Boolean): Flow<List<Habit>> {
            return habits.map { current ->
                if (includeDeleted) current else current.filterNot { it.isDeleted }
            }
        }

        override suspend fun getHabit(
            habitId: String,
            recordDate: Long,
            includeDeleted: Boolean,
        ): Habit? {
            return habits.value.firstOrNull { it.id == habitId && (includeDeleted || !it.isDeleted) }
        }

        override suspend fun saveHabit(habit: Habit, steps: List<luzzr.zou.domain.model.HabitStep>) = Unit

        override suspend fun softDeleteHabit(habitId: String) = Unit

        override suspend fun restoreHabit(habitId: String) {
            habits.value = habits.value.map { habit ->
                if (habit.id == habitId) {
                    habit.copy(isDeleted = false, deletedAt = null)
                } else {
                    habit
                }
            }
        }

        override suspend fun completeCheckHabit(habitId: String, recordDate: Long) {
            habits.value = habits.value.map { habit ->
                if (habit.id == habitId) {
                    habit.copy(
                        todayRecord = HabitRecord(
                            id = "record-$habitId",
                            habitId = habitId,
                            recordDate = recordDate,
                            status = HabitRecordStatus.COMPLETED,
                        ),
                    )
                } else {
                    habit
                }
            }
        }

        override suspend fun completeStepsHabit(habitId: String, recordDate: Long) = Unit

        override suspend fun completeDurationHabit(habitId: String, recordDate: Long, durationMinutes: Int) = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        val state = MutableStateFlow(ReminderPreferences())

        override fun observeReminderPreferences(): Flow<ReminderPreferences> = state

        override suspend fun getReminderPreferences(): ReminderPreferences = state.value

        override suspend fun updateReminderPreferences(transform: (ReminderPreferences) -> ReminderPreferences) {
            state.value = transform(state.value)
        }

        override suspend fun replaceReminderPreferences(preferences: ReminderPreferences) {
            state.value = preferences
        }
    }

    private fun habit(
        id: String,
        title: String,
        frequencyType: HabitFrequencyType,
        selectedWeekdays: Set<DayOfWeek> = emptySet(),
        isDeleted: Boolean = false,
    ): Habit {
        return Habit(
            id = id,
            title = title,
            frequencyType = frequencyType,
            selectedWeekdays = selectedWeekdays,
            checkInMode = HabitCheckInMode.CHECK,
            isDeleted = isDeleted,
        )
    }
}
