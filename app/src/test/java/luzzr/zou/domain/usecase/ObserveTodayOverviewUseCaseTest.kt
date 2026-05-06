package luzzr.zou.domain.usecase

import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitRecord
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.model.Note
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.repository.HabitRepository
import luzzr.zou.domain.repository.NoteRepository
import luzzr.zou.domain.repository.TaskRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveTodayOverviewUseCaseTest {

    @Test
    fun aggregatesTasksHabitsAndRecentNotes() = runTest {
        val taskRepository = FakeTaskRepository()
        val habitRepository = FakeHabitRepository()
        val noteRepository = FakeNoteRepository()
        val timeProvider = FixedTimeProvider()
        val todayStart = timeProvider.todayStartMillis()

        taskRepository.tasks.value = listOf(
            task(id = "task-active-1", title = "Task 1", status = TaskStatus.ACTIVE),
            task(id = "task-active-2", title = "Task 2", status = TaskStatus.ACTIVE),
            task(
                id = "task-completed-today",
                title = "Task 3",
                status = TaskStatus.COMPLETED,
                updatedAt = todayStart + 60_000,
            ),
            task(
                id = "task-completed-old",
                title = "Task 4",
                status = TaskStatus.COMPLETED,
                updatedAt = todayStart - 86_400_000,
            ),
        )
        habitRepository.habits.value = listOf(
            habit(id = "habit-due", title = "Habit Due"),
            habit(
                id = "habit-completed",
                title = "Habit Completed",
                todayRecord = HabitRecord(
                    id = "record-1",
                    habitId = "habit-completed",
                    recordDate = LocalDate.of(2026, 3, 9).toEpochDay(),
                    status = HabitRecordStatus.COMPLETED,
                ),
            ),
            habit(
                id = "habit-not-due",
                title = "Habit Not Due",
                frequencyType = HabitFrequencyType.WEEKLY,
                selectedWeekdays = setOf(DayOfWeek.TUESDAY),
            ),
        )
        noteRepository.notes.value = listOf(
            note(id = "note-1", updatedAt = todayStart + 4_000),
            note(id = "note-2", updatedAt = todayStart + 3_000),
            note(id = "note-3", updatedAt = todayStart + 2_000),
            note(id = "note-4", updatedAt = todayStart + 1_000),
        )

        val useCase = ObserveTodayOverviewUseCase(
            observeTasksUseCase = ObserveTasksUseCase(taskRepository),
            observeHabitsUseCase = ObserveHabitsUseCase(habitRepository),
            observeNotesUseCase = ObserveNotesUseCase(noteRepository),
            habitScheduleEvaluator = HabitScheduleEvaluator(),
            timeProvider = timeProvider,
        )

        val overview = useCase().first()

        assertEquals(2, overview.summary.pendingTaskCount)
        assertEquals(1, overview.summary.dueHabitCount)
        assertEquals(2, overview.summary.completedCount)
        assertEquals(listOf("task-active-1", "task-active-2"), overview.tasks.map { it.task.id })
        assertEquals(
            listOf("habit-due", "habit-completed"),
            overview.habits.map { it.habit.id },
        )
        assertEquals(listOf("note-1", "note-2", "note-3"), overview.recentNotes.map { it.note.id })
        assertTrue(!overview.isCompletelyEmpty)
    }

    private class FixedTimeProvider : TimeProvider {
        override fun nowMillis(): Long = todayStartMillis()

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")

        fun todayStartMillis(): Long = LocalDate.of(2026, 3, 9)
            .atStartOfDay(zoneId())
            .toInstant()
            .toEpochMilli()
    }

    private class FakeTaskRepository : TaskRepository {
        val tasks = MutableStateFlow<List<Task>>(emptyList())

        override fun observeTasks(includeCompleted: Boolean): Flow<List<Task>> {
            return tasks.map { current ->
                if (includeCompleted) current else current.filterNot { it.status == TaskStatus.COMPLETED }
            }
        }

        override suspend fun getTask(taskId: String): Task? = tasks.value.firstOrNull { it.id == taskId }

        override suspend fun saveTask(task: Task, subTasks: List<SubTask>) = Unit

        override suspend fun softDeleteTask(taskId: String) = Unit

        override suspend fun setTaskCompleted(taskId: String, completed: Boolean) = Unit

        override suspend fun setSubTaskCompleted(taskId: String, subTaskId: String, completed: Boolean) = Unit
    }

    private class FakeHabitRepository : HabitRepository {
        val habits = MutableStateFlow<List<Habit>>(emptyList())

        override fun observeHabits(recordDate: Long, includeDeleted: Boolean): Flow<List<Habit>> {
            return habits.map { current ->
                if (includeDeleted) current else current.filterNot { it.isDeleted }
            }
        }

        override suspend fun getHabit(habitId: String, recordDate: Long, includeDeleted: Boolean): Habit? {
            return habits.value.firstOrNull { it.id == habitId }
        }

        override suspend fun saveHabit(habit: Habit, steps: List<luzzr.zou.domain.model.HabitStep>) = Unit

        override suspend fun softDeleteHabit(habitId: String) = Unit

        override suspend fun restoreHabit(habitId: String) = Unit

        override suspend fun completeCheckHabit(habitId: String, recordDate: Long) = Unit

        override suspend fun completeStepsHabit(habitId: String, recordDate: Long) = Unit

        override suspend fun completeDurationHabit(habitId: String, recordDate: Long, durationMinutes: Int) = Unit
    }

    private class FakeNoteRepository : NoteRepository {
        val notes = MutableStateFlow<List<Note>>(emptyList())

        override fun observeNotes(): Flow<List<Note>> = notes

        override suspend fun getNote(noteId: String) = null

        override suspend fun saveNote(note: Note) = Unit

        override suspend fun softDeleteNote(noteId: String) = Unit

        override suspend fun importImage(noteId: String, sourceUri: String) =
            throw UnsupportedOperationException()

        override suspend fun discardDraft(noteId: String) = Unit
    }

    private fun task(
        id: String,
        title: String,
        status: TaskStatus,
        updatedAt: Long = 0L,
    ): Task {
        return Task(
            id = id,
            title = title,
            priority = TaskPriority.NORMAL,
            status = status,
            completionRule = TaskCompletionRule.MANUAL,
            updatedAt = updatedAt,
        )
    }

    private fun habit(
        id: String,
        title: String,
        frequencyType: HabitFrequencyType = HabitFrequencyType.DAILY,
        selectedWeekdays: Set<DayOfWeek> = emptySet(),
        todayRecord: HabitRecord? = null,
    ): Habit {
        return Habit(
            id = id,
            title = title,
            frequencyType = frequencyType,
            selectedWeekdays = selectedWeekdays,
            checkInMode = HabitCheckInMode.CHECK,
            todayRecord = todayRecord,
        )
    }

    private fun note(
        id: String,
        updatedAt: Long,
    ): Note {
        return Note(
            id = id,
            title = id,
            previewText = "preview-$id",
            updatedAt = updatedAt,
        )
    }
}
