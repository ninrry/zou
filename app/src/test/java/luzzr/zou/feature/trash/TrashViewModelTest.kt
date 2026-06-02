package luzzr.zou.feature.trash

import luzzr.zou.MainDispatcherRule
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Note
import luzzr.zou.domain.model.TrashItemType
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.repository.HabitRepository
import luzzr.zou.domain.repository.NoteRepository
import luzzr.zou.domain.repository.TaskRepository
import luzzr.zou.domain.usecase.HardDeleteHabitUseCase
import luzzr.zou.domain.usecase.HardDeleteNoteUseCase
import luzzr.zou.domain.usecase.HardDeleteTaskUseCase
import luzzr.zou.domain.usecase.HardDeleteTrashItemUseCase
import luzzr.zou.domain.usecase.ObserveTrashItemsUseCase
import luzzr.zou.domain.usecase.RestoreHabitUseCase
import luzzr.zou.domain.usecase.RestoreNoteUseCase
import luzzr.zou.domain.usecase.RestoreTaskUseCase
import luzzr.zou.domain.usecase.RestoreTrashItemUseCase
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun mapsDeletedItemsAndHandlesRestore() = runTest {
        val taskRepository = FakeTaskRepository()
        val habitRepository = FakeHabitRepository()
        val noteRepository = FakeNoteRepository()
        taskRepository.deletedTasks.value = listOf(
            Task(
                id = "task-1",
                title = "Deleted task",
                priority = TaskPriority.NORMAL,
                status = TaskStatus.ACTIVE,
                completionRule = TaskCompletionRule.MANUAL,
                isDeleted = true,
                deletedAt = 1_730_000_100_000L,
            ),
        )

        val viewModel = TrashViewModel(
            observeTrashItemsUseCase = ObserveTrashItemsUseCase(
                taskRepository = taskRepository,
                habitRepository = habitRepository,
                noteRepository = noteRepository,
                timeProvider = FixedTimeProvider(),
            ),
            restoreTrashItemUseCase = RestoreTrashItemUseCase(
                restoreTaskUseCase = RestoreTaskUseCase(taskRepository),
                restoreHabitUseCase = RestoreHabitUseCase(habitRepository),
                restoreNoteUseCase = RestoreNoteUseCase(noteRepository),
            ),
            hardDeleteTrashItemUseCase = HardDeleteTrashItemUseCase(
                hardDeleteTaskUseCase = HardDeleteTaskUseCase(taskRepository),
                hardDeleteHabitUseCase = HardDeleteHabitUseCase(habitRepository),
                hardDeleteNoteUseCase = HardDeleteNoteUseCase(noteRepository),
            ),
            timeProvider = FixedTimeProvider(),
        )
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.items.size)

        viewModel.restore(viewModel.uiState.value.items.single())
        advanceUntilIdle()

        assertEquals(listOf("task-1"), taskRepository.restoredIds)
        assertTrue(viewModel.uiState.value.resultMessage?.contains("Deleted task") == true)
        collectJob.cancel()
    }

    @Test
    fun surfacesHardDeleteFailure() = runTest {
        val taskRepository = FakeTaskRepository(throwOnHardDelete = true)
        val habitRepository = FakeHabitRepository()
        val noteRepository = FakeNoteRepository()
        taskRepository.deletedTasks.value = listOf(
            Task(
                id = "task-1",
                title = "Deleted task",
                priority = TaskPriority.NORMAL,
                status = TaskStatus.ACTIVE,
                completionRule = TaskCompletionRule.MANUAL,
                isDeleted = true,
                deletedAt = 1_730_000_100_000L,
            ),
        )

        val viewModel = TrashViewModel(
            observeTrashItemsUseCase = ObserveTrashItemsUseCase(
                taskRepository = taskRepository,
                habitRepository = habitRepository,
                noteRepository = noteRepository,
                timeProvider = FixedTimeProvider(),
            ),
            restoreTrashItemUseCase = RestoreTrashItemUseCase(
                restoreTaskUseCase = RestoreTaskUseCase(taskRepository),
                restoreHabitUseCase = RestoreHabitUseCase(habitRepository),
                restoreNoteUseCase = RestoreNoteUseCase(noteRepository),
            ),
            hardDeleteTrashItemUseCase = HardDeleteTrashItemUseCase(
                hardDeleteTaskUseCase = HardDeleteTaskUseCase(taskRepository),
                hardDeleteHabitUseCase = HardDeleteHabitUseCase(habitRepository),
                hardDeleteNoteUseCase = HardDeleteNoteUseCase(noteRepository),
            ),
            timeProvider = FixedTimeProvider(),
        )
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()
        viewModel.hardDelete(viewModel.uiState.value.items.single())
        advanceUntilIdle()

        assertEquals("hard delete failed", viewModel.uiState.value.errorMessage)
        collectJob.cancel()
    }

    private class FixedTimeProvider : TimeProvider {
        override fun nowMillis(): Long = 1_730_000_000_000L

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")

        fun epochDay(): Long = java.time.LocalDate.of(2026, 3, 9).toEpochDay()
    }

    private class FakeTaskRepository(
        private val throwOnHardDelete: Boolean = false,
    ) : TaskRepository {
        val deletedTasks = MutableStateFlow<List<Task>>(emptyList())
        val restoredIds = mutableListOf<String>()

        override fun observeTasks(includeCompleted: Boolean): Flow<List<Task>> = flowOf(emptyList())

        override fun observeDeletedTasks(): Flow<List<Task>> = deletedTasks

        override suspend fun getTask(taskId: String): Task? = null

        override suspend fun saveTask(task: Task, subTasks: List<SubTask>) = Unit

        override suspend fun softDeleteTask(taskId: String) = Unit

        override suspend fun restoreTask(taskId: String) {
            restoredIds += taskId
        }

        override suspend fun hardDeleteTask(taskId: String) {
            if (throwOnHardDelete) error("hard delete failed")
        }

        override suspend fun setTaskCompleted(taskId: String, completed: Boolean) = Unit

        override suspend fun setSubTaskCompleted(taskId: String, subTaskId: String, completed: Boolean) = Unit
    }

    private class FakeHabitRepository : HabitRepository {
        override fun observeHabits(recordDate: Long, includeDeleted: Boolean): Flow<List<Habit>> = flowOf(emptyList())

        override fun observeDeletedHabits(recordDate: Long): Flow<List<Habit>> = flowOf(emptyList())

        override suspend fun getHabit(habitId: String, recordDate: Long, includeDeleted: Boolean): Habit? = null

        override suspend fun saveHabit(habit: Habit, steps: List<luzzr.zou.domain.model.HabitStep>) = Unit

        override suspend fun softDeleteHabit(habitId: String) = Unit

        override suspend fun restoreHabit(habitId: String) = Unit

        override suspend fun hardDeleteHabit(habitId: String) = Unit

        override suspend fun completeCheckHabit(habitId: String, recordDate: Long) = Unit

        override suspend fun completeStepsHabit(habitId: String, recordDate: Long) = Unit

        override suspend fun completeDurationHabit(habitId: String, recordDate: Long, durationMinutes: Int) = Unit
    }

    private class FakeNoteRepository : NoteRepository {
        override fun observeNotes(): Flow<List<Note>> = flowOf(emptyList())

        override fun observeDeletedNotes(): Flow<List<Note>> = flowOf(emptyList())

        override suspend fun getNote(noteId: String) = null

        override suspend fun saveNote(note: Note) = Unit

        override suspend fun softDeleteNote(noteId: String) = Unit

        override suspend fun restoreNote(noteId: String) = Unit

        override suspend fun hardDeleteNote(noteId: String) = Unit

        override suspend fun importImage(noteId: String, sourceUri: String) =
            throw UnsupportedOperationException()

        override suspend fun bulkPinNotes(noteIds: List<String>, isPinned: Boolean) = Unit

        override suspend fun bulkSoftDeleteNotes(noteIds: List<String>) = Unit

        override suspend fun discardDraft(noteId: String) = Unit
    }
}
