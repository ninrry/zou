package luzzr.zou.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.usecase.ObserveReminderPreferencesUseCase
import luzzr.zou.domain.usecase.ObserveTasksUseCase
import luzzr.zou.domain.usecase.ToggleTaskCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel @Inject constructor(
    observeReminderPreferencesUseCase: ObserveReminderPreferencesUseCase,
    private val observeTasksUseCase: ObserveTasksUseCase,
    private val toggleTaskCompletedUseCase: ToggleTaskCompletedUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    val uiState: StateFlow<TasksUiState> = observeReminderPreferencesUseCase()
        .map { preferences -> preferences.showCompletedTasks }
        .flatMapLatest { includeCompleted ->
            observeTasksUseCase(includeCompleted = includeCompleted)
                .map { tasks ->
                    TasksUiState(
                        showCompleted = includeCompleted,
                        tasks = tasks.map(::mapTask),
                        emptyTitle = if (includeCompleted) {
                            "暂无已完成任务"
                        } else {
                            "还没有待办"
                        },
                        emptyDescription = if (includeCompleted) {
                            "完成任务后会在这里显示，方便你随时恢复。"
                        } else {
                            "点击右下角 + 创建第一条任务，设置截止时间与提醒。"
                        },
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TasksUiState(),
        )

    fun onTaskCompletionToggle(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            toggleTaskCompletedUseCase(
                taskId = taskId,
                completed = completed,
            )
        }
    }

    private fun mapTask(task: Task): TaskListItemUiModel {
        val zoneId = timeProvider.zoneId()
        val now = timeProvider.nowMillis()
        val dueText = when {
            task.status == TaskStatus.COMPLETED -> "已完成"
            task.dueAt == null -> "无截止时间"
            task.dueAt < now -> "已逾期 · ${task.dueAt.format(dateTimeFormatter, zoneId)}"
            task.dueAt.isToday(now, zoneId) -> "今天到期 · ${task.dueAt.format(dateTimeFormatter, zoneId)}"
            task.dueAt.isTomorrow(now, zoneId) -> "明天到期 · ${task.dueAt.format(dateTimeFormatter, zoneId)}"
            task.dueAt.isDayAfterTomorrow(now, zoneId) -> {
                "后天到期 · ${task.dueAt.format(dateTimeFormatter, zoneId)}"
            }
            else -> task.dueAt.format(dateTimeFormatter, zoneId)
        }
        val completedSubTasks = task.subTasks.count { it.isCompleted }
        return TaskListItemUiModel(
            id = task.id,
            title = task.title,
            priorityLabel = when (task.priority) {
                TaskPriority.NORMAL -> "普通"
                TaskPriority.HIGH -> "高优先级"
                TaskPriority.URGENT -> "紧急优先级"
            },
            showUrgentBadge = task.isUrgent || task.priority == TaskPriority.URGENT,
            dueText = dueText,
            progressText = if (task.subTasks.isEmpty()) {
                ""
            } else {
                "$completedSubTasks/${task.subTasks.size} 子任务"
            },
            isCompleted = task.status == TaskStatus.COMPLETED,
            canToggleCompletion = task.completionRule == TaskCompletionRule.MANUAL || task.subTasks.isEmpty(),
        )
    }

    private fun Long.format(
        formatter: DateTimeFormatter,
        zoneId: java.time.ZoneId,
    ): String {
        return Instant.ofEpochMilli(this).atZone(zoneId).format(formatter)
    }

    private fun Long.isToday(now: Long, zoneId: java.time.ZoneId): Boolean {
        val nowDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val dueDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
        return dueDate == nowDate
    }

    private fun Long.isTomorrow(now: Long, zoneId: java.time.ZoneId): Boolean {
        val nowDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val dueDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
        return dueDate == nowDate.plusDays(1)
    }

    private fun Long.isDayAfterTomorrow(now: Long, zoneId: java.time.ZoneId): Boolean {
        val nowDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val dueDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
        return dueDate == nowDate.plusDays(2)
    }
}
