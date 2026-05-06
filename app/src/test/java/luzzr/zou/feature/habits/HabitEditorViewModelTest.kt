package luzzr.zou.feature.habits

import androidx.lifecycle.SavedStateHandle
import luzzr.zou.MainDispatcherRule
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitStep
import luzzr.zou.domain.repository.HabitRepository
import luzzr.zou.domain.repository.SettingsRepository
import luzzr.zou.domain.usecase.GetHabitUseCase
import luzzr.zou.domain.usecase.GetReminderPreferencesUseCase
import luzzr.zou.domain.usecase.SaveHabitUseCase
import luzzr.zou.domain.usecase.SoftDeleteHabitUseCase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun blocksSaveWhenTitleIsBlank() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)

        val valid = viewModel.validateBeforeSave()

        assertFalse(valid)
        assertEquals("标题不能为空", viewModel.uiState.value.titleError)
        assertEquals("标题不能为空", viewModel.uiState.value.saveErrorMessage)
    }

    @Test
    fun blocksWeeklyHabitWithoutWeekdaySelection() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("Read")
        viewModel.onFrequencySelected(HabitFrequencyType.WEEKLY)

        val valid = viewModel.validateBeforeSave()

        assertFalse(valid)
        assertEquals("请至少选择一个执行日", viewModel.uiState.value.frequencyError)
        assertEquals("请至少选择一个执行日", viewModel.uiState.value.saveErrorMessage)
    }

    @Test
    fun saveHabitShowsSavingStateAndPersistsData() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)

        viewModel.onTitleChanged("Workout")
        viewModel.onFrequencySelected(HabitFrequencyType.MONTHLY)
        viewModel.onMonthlyDaysChanged("1,15,15")
        viewModel.onCheckInModeSelected(HabitCheckInMode.DURATION)
        viewModel.onTargetDurationChanged("30")

        viewModel.saveHabit()

        assertTrue(viewModel.uiState.value.isSaving)
        advanceUntilIdle()

        val savedHabit = repository.savedHabit
        assertNotNull(savedHabit)
        assertEquals(setOf(1, 15), savedHabit?.monthlyDays)
        assertEquals(30, savedHabit?.targetDurationMinutes)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun blocksStepsModeWithoutAnyNonBlankStep() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("Practice")
        viewModel.onCheckInModeSelected(HabitCheckInMode.STEPS)
        viewModel.onAddStep()

        val valid = viewModel.validateBeforeSave()

        assertFalse(valid)
        assertEquals("步骤型习惯至少需要一个步骤", viewModel.uiState.value.stepsError)
        assertEquals("步骤型习惯至少需要一个步骤", viewModel.uiState.value.saveErrorMessage)
    }

    @Test
    fun blocksPastExactReminderForTodayOnNewHabit() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)

        viewModel.onTitleChanged("Walk")
        viewModel.onFrequencySelected(HabitFrequencyType.DAILY)
        viewModel.onAddExactReminder(java.time.LocalTime.of(9, 0))

        val valid = viewModel.validateBeforeSave()

        assertFalse(valid)
        assertEquals("今天的特别提醒时间不能早于当前时间", viewModel.uiState.value.reminderError)
        assertEquals("今天的特别提醒时间不能早于当前时间", viewModel.uiState.value.saveErrorMessage)
    }

    @Test
    fun softDeleteIsOnlyAvailableInEditMode() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)

        viewModel.onDeleteClicked()
        advanceUntilIdle()

        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun exposesLoadErrorWhenEditingMissingHabit() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = HabitEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf(HabitRoutes.habitIdArg to "missing")),
            getHabitUseCase = GetHabitUseCase(repository),
            getReminderPreferencesUseCase = GetReminderPreferencesUseCase(FakeSettingsRepository()),
            saveHabitUseCase = SaveHabitUseCase(repository),
            softDeleteHabitUseCase = SoftDeleteHabitUseCase(repository),
            timeProvider = FixedTimeProvider(),
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasMissingContent)
        assertEquals("习惯不存在或已删除。", viewModel.uiState.value.loadErrorMessage)
    }

    private fun createViewModel(repository: FakeHabitRepository): HabitEditorViewModel {
        return HabitEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            getHabitUseCase = GetHabitUseCase(repository),
            getReminderPreferencesUseCase = GetReminderPreferencesUseCase(FakeSettingsRepository()),
            saveHabitUseCase = SaveHabitUseCase(repository),
            softDeleteHabitUseCase = SoftDeleteHabitUseCase(repository),
            timeProvider = FixedTimeProvider(),
        )
    }

    private class FixedTimeProvider : TimeProvider {
        override fun nowMillis(): Long = LocalDate.of(2026, 3, 9)
            .atTime(12, 0)
            .atZone(ZoneId.of("Asia/Singapore"))
            .toInstant()
            .toEpochMilli()

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }

    private class FakeHabitRepository : HabitRepository {
        val habits = MutableStateFlow<List<Habit>>(emptyList())
        var savedHabit: Habit? = null
        var savedSteps: List<HabitStep> = emptyList()
        val deletedIds = mutableListOf<String>()

        override fun observeHabits(recordDate: Long, includeDeleted: Boolean): Flow<List<Habit>> = habits
        override suspend fun getHabit(habitId: String, recordDate: Long, includeDeleted: Boolean): Habit? = habits.value.firstOrNull { it.id == habitId }
        override suspend fun saveHabit(habit: Habit, steps: List<HabitStep>) {
            savedHabit = habit
            savedSteps = steps
        }
        override suspend fun softDeleteHabit(habitId: String) { deletedIds += habitId }
        override suspend fun restoreHabit(habitId: String) = Unit
        override suspend fun completeCheckHabit(habitId: String, recordDate: Long) = Unit
        override suspend fun completeStepsHabit(habitId: String, recordDate: Long) = Unit
        override suspend fun completeDurationHabit(habitId: String, recordDate: Long, durationMinutes: Int) = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        override fun observeReminderPreferences(): Flow<ReminderPreferences> = kotlinx.coroutines.flow.emptyFlow()
        override suspend fun getReminderPreferences(): ReminderPreferences = ReminderPreferences()
        override suspend fun updateReminderPreferences(transform: (ReminderPreferences) -> ReminderPreferences) = Unit
        override suspend fun replaceReminderPreferences(preferences: ReminderPreferences) = Unit
    }
}
