package luzzr.zou.feature.tasks

import luzzr.zou.MainDispatcherRule
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.repository.SettingsRepository
import luzzr.zou.domain.repository.TaskRepository
import luzzr.zou.domain.usecase.ObserveReminderPreferencesUseCase
import luzzr.zou.domain.usecase.ObserveTasksUseCase
import luzzr.zou.domain.usecase.ToggleTaskCompletedUseCase
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
class TasksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun hidesCompletedTasksByDefaultAndShowsThemWhenEnabled() = runTest {
        val repository = FakeTaskRepository()
        val settingsRepository = FakeSettingsRepository()
        repository.tasks.value = listOf(
            task(id = "1", title = "未完成任务", status = TaskStatus.ACTIVE),
            task(id = "2", title = "已完成任务", status = TaskStatus.COMPLETED),
        )

        val viewModel = TasksViewModel(
            observeReminderPreferencesUseCase = ObserveReminderPreferencesUseCase(settingsRepository),
            observeTasksUseCase = ObserveTasksUseCase(repository),
            toggleTaskCompletedUseCase = ToggleTaskCompletedUseCase(repository),
            timeProvider = FixedTimeProvider(),
        )
        val job = backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()
        assertEquals(listOf("未完成任务"), viewModel.uiState.value.tasks.map { it.title })

        settingsRepository.state.value = settingsRepository.state.value.copy(showCompletedTasks = true)
        advanceUntilIdle()

        assertEquals(
            listOf("未完成任务", "已完成任务"),
            viewModel.uiState.value.tasks.map { it.title },
        )

        job.cancel()
    }

    private class FixedTimeProvider : TimeProvider {
        override fun nowMillis(): Long = 1_730_000_000_000L

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }

    private class FakeTaskRepository : TaskRepository {
        val tasks = MutableStateFlow<List<Task>>(emptyList())

        override fun observeTasks(includeCompleted: Boolean): Flow<List<Task>> {
            return tasks.map { value ->
                if (includeCompleted) {
                    value
                } else {
                    value.filterNot { it.status == TaskStatus.COMPLETED }
                }
            }
        }

        override suspend fun getTask(taskId: String): Task? = tasks.value.firstOrNull { it.id == taskId }

        override suspend fun saveTask(task: Task, subTasks: List<SubTask>) = Unit

        override suspend fun softDeleteTask(taskId: String) = Unit

        override suspend fun setTaskCompleted(taskId: String, completed: Boolean) = Unit

        override suspend fun setSubTaskCompleted(taskId: String, subTaskId: String, completed: Boolean) = Unit
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

    private fun task(
        id: String,
        title: String,
        status: TaskStatus,
    ): Task {
        return Task(
            id = id,
            title = title,
            priority = TaskPriority.NORMAL,
            status = status,
            completionRule = TaskCompletionRule.MANUAL,
        )
    }
}
