package luzzr.zou.feature.today

import luzzr.zou.MainDispatcherRule
import luzzr.zou.core.time.RemainingTimeFormatter
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitStepAdvanceResult
import luzzr.zou.domain.model.InsertedNoteImage
import luzzr.zou.domain.model.Note
import luzzr.zou.domain.model.NoteDetail
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.model.TaskSubTaskAdvanceResult
import luzzr.zou.domain.repository.HabitRepository
import luzzr.zou.domain.repository.NoteRepository
import luzzr.zou.domain.repository.TaskRepository
import luzzr.zou.domain.usecase.AdvanceHabitStepUseCase
import luzzr.zou.domain.usecase.AdvanceTaskSubTaskUseCase
import luzzr.zou.domain.usecase.CheckInPolicy
import luzzr.zou.domain.usecase.CompleteCheckHabitUseCase
import luzzr.zou.domain.usecase.FinishHabitDurationUseCase
import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.HabitScheduleEvaluator
import luzzr.zou.domain.usecase.ObserveHabitsUseCase
import luzzr.zou.domain.usecase.ObserveNotesUseCase
import luzzr.zou.domain.usecase.ObserveTasksUseCase
import luzzr.zou.domain.usecase.ObserveTodayOverviewUseCase
import luzzr.zou.domain.usecase.PauseHabitDurationUseCase
import luzzr.zou.domain.usecase.RevertHabitStepUseCase
import luzzr.zou.domain.usecase.StartHabitDurationUseCase
import luzzr.zou.domain.usecase.ToggleSubTaskCompletedUseCase
import luzzr.zou.domain.usecase.ToggleTaskCompletedUseCase
import luzzr.zou.domain.usecase.UndoHabitCompletionUseCase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun maps_quick_cards_and_counts_in_same_scope() = runTest {
        val timeProvider = FixedTimeProvider(LocalDate.of(2026, 3, 13))
        val taskRepository = FakeTaskRepository(
            tasks = listOf(
                Task(
                    id = "task-1",
                    title = "提交周报",
                    status = TaskStatus.ACTIVE,
                    completionRule = TaskCompletionRule.MANUAL,
                    dueAt = timeProvider.nowMillis() + 2 * 60 * 60 * 1_000L,
                ),
            ),
        )
        val habitRepository = FakeHabitRepository(
            habits = listOf(
                Habit(
                    id = "habit-1",
                    title = "喝水",
                    checkInMode = HabitCheckInMode.CHECK,
                ),
            ),
        )
        val noteRepository = FakeNoteRepository()
        val viewModel = createViewModel(taskRepository, habitRepository, noteRepository, timeProvider)

        primeUiState(viewModel)

        val state = viewModel.uiState.value
        assertEquals(1, state.summary.pendingTaskCount)
        assertEquals(1, state.summary.dueHabitCount)
        assertEquals(1, state.tasks.size)
        assertEquals(1, state.habits.size)
        assertTrue(state.tasks.first().remainingTimeText?.isNotBlank() == true)
    }

    @Test
    fun task_and_habit_quick_actions_dispatch_repository_operations() = runTest {
        val timeProvider = FixedTimeProvider(LocalDate.of(2026, 3, 13))
        val taskRepository = FakeTaskRepository(
            tasks = listOf(
                Task(
                    id = "task-1",
                    title = "提交周报",
                    status = TaskStatus.ACTIVE,
                    completionRule = TaskCompletionRule.MANUAL,
                ),
            ),
        )
        val habitRepository = FakeHabitRepository(
            habits = listOf(
                Habit(
                    id = "habit-1",
                    title = "喝水",
                    checkInMode = HabitCheckInMode.CHECK,
                ),
            ),
        )
        val noteRepository = FakeNoteRepository()
        val viewModel = createViewModel(taskRepository, habitRepository, noteRepository, timeProvider)
        primeUiState(viewModel)

        viewModel.onTaskAction("task-1", luzzr.zou.domain.usecase.TaskQuickActionType.COMPLETE)
        viewModel.onHabitPrimaryAction(
            habitId = "habit-1",
            actionType = HabitQuickActionType.CHECK,
            durationRunning = false,
        )
        advanceUntilIdle()

        assertEquals(listOf("task-1:true"), taskRepository.taskCompleteEvents)
        assertEquals(listOf("habit-1"), habitRepository.checkCompleteEvents)
    }

    private fun createViewModel(
        taskRepository: FakeTaskRepository,
        habitRepository: FakeHabitRepository,
        noteRepository: FakeNoteRepository,
        timeProvider: TimeProvider,
    ): TodayViewModel {
        val checkInPolicy = CheckInPolicy(timeProvider)
        return TodayViewModel(
            observeTodayOverviewUseCase = ObserveTodayOverviewUseCase(
                observeTasksUseCase = ObserveTasksUseCase(taskRepository),
                observeHabitsUseCase = ObserveHabitsUseCase(habitRepository),
                observeNotesUseCase = ObserveNotesUseCase(noteRepository),
                habitScheduleEvaluator = HabitScheduleEvaluator(),
                timeProvider = timeProvider,
            ),
            toggleTaskCompletedUseCase = ToggleTaskCompletedUseCase(taskRepository),
            toggleSubTaskCompletedUseCase = ToggleSubTaskCompletedUseCase(taskRepository),
            advanceTaskSubTaskUseCase = AdvanceTaskSubTaskUseCase(taskRepository),
            completeCheckHabitUseCase = CompleteCheckHabitUseCase(habitRepository),
            advanceHabitStepUseCase = AdvanceHabitStepUseCase(habitRepository),
            revertHabitStepUseCase = RevertHabitStepUseCase(habitRepository),
            startHabitDurationUseCase = StartHabitDurationUseCase(habitRepository),
            pauseHabitDurationUseCase = PauseHabitDurationUseCase(habitRepository),
            finishHabitDurationUseCase = FinishHabitDurationUseCase(habitRepository),
            undoHabitCompletionUseCase = UndoHabitCompletionUseCase(habitRepository),
            timeProvider = timeProvider,
            quickPreviewPolicy = TodayQuickPreviewPolicy(checkInPolicy),
            remainingTimeFormatter = RemainingTimeFormatter(),
        )
    }

    private fun TestScope.primeUiState(viewModel: TodayViewModel) {
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        runCurrent()
        collectJob.cancel()
        runCurrent()
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

    private class FakeTaskRepository(
        tasks: List<Task> = emptyList(),
    ) : TaskRepository {
        private val taskFlow = MutableStateFlow(tasks)
        val taskCompleteEvents = mutableListOf<String>()

        override fun observeTasks(includeCompleted: Boolean): Flow<List<Task>> {
            return taskFlow.map { current ->
                if (includeCompleted) current else current.filterNot { it.status == TaskStatus.COMPLETED }
            }
        }

        override suspend fun getTask(taskId: String): Task? = taskFlow.value.firstOrNull { it.id == taskId }

        override suspend fun saveTask(task: Task, subTasks: List<SubTask>) = Unit

        override suspend fun softDeleteTask(taskId: String) = Unit

        override suspend fun setTaskCompleted(taskId: String, completed: Boolean) {
            taskCompleteEvents += "$taskId:$completed"
        }

        override suspend fun setSubTaskCompleted(taskId: String, subTaskId: String, completed: Boolean) = Unit

        override suspend fun advanceTaskSubTask(taskId: String): TaskSubTaskAdvanceResult {
            return TaskSubTaskAdvanceResult()
        }
    }

    private class FakeHabitRepository(
        habits: List<Habit> = emptyList(),
    ) : HabitRepository {
        private val habitFlow = MutableStateFlow(habits)
        val checkCompleteEvents = mutableListOf<String>()

        override fun observeHabits(recordDate: Long, includeDeleted: Boolean): Flow<List<Habit>> = habitFlow

        override suspend fun getHabit(
            habitId: String,
            recordDate: Long,
            includeDeleted: Boolean,
        ): Habit? = habitFlow.value.firstOrNull { it.id == habitId }

        override suspend fun saveHabit(
            habit: Habit,
            steps: List<luzzr.zou.domain.model.HabitStep>,
        ) = Unit

        override suspend fun softDeleteHabit(habitId: String) = Unit

        override suspend fun restoreHabit(habitId: String) = Unit

        override suspend fun completeCheckHabit(habitId: String, recordDate: Long) {
            checkCompleteEvents += habitId
        }

        override suspend fun completeStepsHabit(habitId: String, recordDate: Long) = Unit

        override suspend fun completeDurationHabit(habitId: String, recordDate: Long, durationMinutes: Int) = Unit

        override suspend fun advanceHabitStep(habitId: String, recordDate: Long): HabitStepAdvanceResult {
            return HabitStepAdvanceResult()
        }
    }

    private class FakeNoteRepository : NoteRepository {
        override fun observeNotes(): Flow<List<Note>> = MutableStateFlow(emptyList())

        override suspend fun getNote(noteId: String): NoteDetail? = null

        override suspend fun saveNote(note: Note) = Unit

        override suspend fun softDeleteNote(noteId: String) = Unit

        override suspend fun importImage(noteId: String, sourceUri: String): InsertedNoteImage {
            error("Not used")
        }

        override suspend fun bulkPinNotes(noteIds: List<String>, isPinned: Boolean) = Unit

        override suspend fun bulkSoftDeleteNotes(noteIds: List<String>) = Unit

        override suspend fun discardDraft(noteId: String) = Unit
    }
}
