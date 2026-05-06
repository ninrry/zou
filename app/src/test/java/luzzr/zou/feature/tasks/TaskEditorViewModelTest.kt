package luzzr.zou.feature.tasks

import androidx.lifecycle.SavedStateHandle
import luzzr.zou.MainDispatcherRule
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.repository.SettingsRepository
import luzzr.zou.domain.repository.TaskRepository
import luzzr.zou.domain.usecase.GetReminderPreferencesUseCase
import luzzr.zou.domain.usecase.GetTaskUseCase
import luzzr.zou.domain.usecase.SaveTaskUseCase
import luzzr.zou.domain.usecase.SoftDeleteTaskUseCase
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun blankTitleShowsValidationError() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        val isValid = viewModel.validateBeforeSave()

        assertFalse(isValid)
        assertEquals("标题不能为空", viewModel.uiState.value.titleError)
        assertEquals("标题不能为空", viewModel.uiState.value.saveErrorMessage)
    }

    @Test
    fun repeatReminderWithoutStartShowsValidationError() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        viewModel.onTitleChanged("准备任务提醒")
        viewModel.onRepeatIntervalChanged("15")

        val isValid = viewModel.validateBeforeSave()

        assertFalse(isValid)
        assertEquals("开启重复提醒前需要先设置开始提醒时间", viewModel.uiState.value.reminderError)
        assertEquals("开启重复提醒前需要先设置开始提醒时间", viewModel.uiState.value.saveErrorMessage)
    }

    @Test
    fun newTaskBlocksPastDueTime() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        viewModel.onTitleChanged("过去时间任务")
        viewModel.onDueDateSelected(2024, 10, 26)
        viewModel.onDueTimeSelected(10, 0)

        val isValid = viewModel.validateBeforeSave()

        assertFalse(isValid)
        assertEquals("截止时间不能早于当前时间", viewModel.uiState.value.dueAtError)
        assertEquals("截止时间不能早于当前时间", viewModel.uiState.value.saveErrorMessage)
    }

    @Test
    fun autoCompletionRuleMarksTaskCompletedWhenAllSubTasksDone() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        viewModel.onTitleChanged("自动完成任务")
        viewModel.onAddSubTask()
        val firstSubTaskId = viewModel.uiState.value.subTasks.first().id
        viewModel.onSubTaskTitleChanged(firstSubTaskId, "步骤一")
        viewModel.onCompletionRuleSelected(TaskCompletionRule.AUTO_ALL_SUBTASKS)
        viewModel.onSubTaskCompletedChanged(firstSubTaskId, true)

        assertTrue(viewModel.uiState.value.isCompleted)
    }

    @Test
    fun saveTaskSortsAndDeduplicatesExactReminderTimes() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        viewModel.onTitleChanged("带提醒的任务")
        viewModel.onAddExactReminder(1_730_000_900_000L)
        viewModel.onAddExactReminder(1_730_000_600_000L)
        viewModel.onAddExactReminder(1_730_000_900_000L)

        viewModel.saveTask()
        advanceUntilIdle()

        assertEquals(
            listOf(1_730_000_600_000L, 1_730_000_900_000L),
            repository.savedTask?.exactReminderTimes,
        )
    }

    @Test
    fun canAddEditAndRemoveSubTaskInEditorState() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        viewModel.onAddSubTask()
        val subTaskId = viewModel.uiState.value.subTasks.single().id
        viewModel.onSubTaskTitleChanged(subTaskId, "准备材料")

        assertEquals("准备材料", viewModel.uiState.value.subTasks.single().title)

        viewModel.onRemoveSubTask(subTaskId)

        assertTrue(viewModel.uiState.value.subTasks.isEmpty())
    }

    @Test
    fun exposesLoadErrorWhenEditingMissingTask() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = TaskEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TaskRoutes.taskIdArg to "missing")),
            getTaskUseCase = GetTaskUseCase(repository),
            getReminderPreferencesUseCase = GetReminderPreferencesUseCase(FakeSettingsRepository()),
            saveTaskUseCase = SaveTaskUseCase(repository),
            softDeleteTaskUseCase = SoftDeleteTaskUseCase(repository),
            timeProvider = FixedTimeProvider(),
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasMissingContent)
        assertEquals("任务不存在或已删除。", viewModel.uiState.value.loadErrorMessage)
    }

    private fun createViewModel(repository: FakeTaskRepository): TaskEditorViewModel {
        return TaskEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            getTaskUseCase = GetTaskUseCase(repository),
            getReminderPreferencesUseCase = GetReminderPreferencesUseCase(FakeSettingsRepository()),
            saveTaskUseCase = SaveTaskUseCase(repository),
            softDeleteTaskUseCase = SoftDeleteTaskUseCase(repository),
            timeProvider = FixedTimeProvider(),
        )
    }

    private class FixedTimeProvider : TimeProvider {
        override fun nowMillis(): Long = 1_730_000_000_000L
        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }

    private class FakeTaskRepository : TaskRepository {
        var savedTask: Task? = null

        override fun observeTasks(includeCompleted: Boolean): Flow<List<Task>> = emptyFlow()
        override suspend fun getTask(taskId: String): Task? = null
        override suspend fun saveTask(task: Task, subTasks: List<SubTask>) {
            savedTask = task.copy(subTasks = subTasks)
        }
        override suspend fun softDeleteTask(taskId: String) = Unit
        override suspend fun setTaskCompleted(taskId: String, completed: Boolean) = Unit
        override suspend fun setSubTaskCompleted(taskId: String, subTaskId: String, completed: Boolean) = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        override fun observeReminderPreferences(): Flow<ReminderPreferences> = emptyFlow()
        override suspend fun getReminderPreferences(): ReminderPreferences = ReminderPreferences()
        override suspend fun updateReminderPreferences(transform: (ReminderPreferences) -> ReminderPreferences) = Unit
        override suspend fun replaceReminderPreferences(preferences: ReminderPreferences) = Unit
    }
}
