package luzzr.zou.feature.tasks

import androidx.lifecycle.SavedStateHandle
import luzzr.zou.MainDispatcherRule
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.repository.TaskRepository
import luzzr.zou.domain.usecase.GetTaskUseCase
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun mapsTaskToDetailUiState() = runTest {
        val task = Task(
            id = "task-1",
            title = "任务详情测试",
            contentMarkdown = "## 内容",
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE,
            startReminderMinuteOfDay = 9 * 60,
            windowEndMinuteOfDay = 22 * 60,
            dueAt = 1_773_424_800_000L,
            repeatIntervalMinutes = 30,
            exactReminderTimes = listOf(1_773_416_400_000L),
            completionRule = TaskCompletionRule.MANUAL,
            subTasks = listOf(
                SubTask(id = "sub-1", title = "步骤一", isCompleted = true),
                SubTask(id = "sub-2", title = "步骤二", isCompleted = false),
            ),
        )
        val viewModel = TaskDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TaskRoutes.taskIdArg to "task-1")),
            getTaskUseCase = GetTaskUseCase(FakeTaskRepository(task)),
            timeProvider = FixedTimeProvider(),
        )

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals("任务详情测试", uiState.title)
        assertEquals("进行中", uiState.statusText)
        assertEquals("1/2 子任务已完成", uiState.progressText)
        assertEquals("## 内容", uiState.contentMarkdown)
        assertTrue(uiState.reminderSummary.any { it.contains("开始提醒时间") })
        assertTrue(uiState.reminderSummary.any { it.contains("特别提醒") })
    }

    private class FixedTimeProvider : TimeProvider {
        override fun nowMillis(): Long = 1_773_410_400_000L

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }

    private class FakeTaskRepository(
        private val task: Task?,
    ) : TaskRepository {
        override fun observeTasks(includeCompleted: Boolean): Flow<List<Task>> = emptyFlow()

        override suspend fun getTask(taskId: String): Task? = task?.takeIf { it.id == taskId }

        override suspend fun saveTask(task: Task, subTasks: List<SubTask>) = Unit

        override suspend fun softDeleteTask(taskId: String) = Unit

        override suspend fun setTaskCompleted(taskId: String, completed: Boolean) = Unit

        override suspend fun setSubTaskCompleted(taskId: String, subTaskId: String, completed: Boolean) = Unit
    }
}
