package luzzr.zou.feature.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.usecase.GetTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTaskUseCase: GetTaskUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val taskId: String? = savedStateHandle[TaskRoutes.taskIdArg]
    private val _uiState = MutableStateFlow(TaskDetailUiState())

    val uiState = _uiState.asStateFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        val targetTaskId = taskId
        if (targetTaskId.isNullOrBlank()) {
            _uiState.value = TaskDetailUiState(
                isLoading = false,
                emptyMessage = "任务不存在或已被删除。",
            )
            return
        }
        viewModelScope.launch {
            val task = getTaskUseCase(targetTaskId)
            _uiState.update {
                task?.toUiState() ?: TaskDetailUiState(
                    isLoading = false,
                    emptyMessage = "任务不存在或已被删除。",
                )
            }
        }
    }

    private fun Task.toUiState(): TaskDetailUiState {
        val zoneId = timeProvider.zoneId()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
        val now = timeProvider.nowMillis()

        val reminderLines = buildList {
            startReminderMinuteOfDay?.let {
                add("开始提醒时间：${it.toDisplayTime()}")
            }
            windowEndMinuteOfDay?.let {
                add("提醒窗口结束：${it.toDisplayTime()}")
            }
            repeatIntervalMinutes?.let {
                add("未完成重复提醒：每 $it 分钟")
            }
            if (exactReminderTimes.isNotEmpty()) {
                add("特别提醒：")
                exactReminderTimes.forEach { exactTime ->
                    add("• ${Instant.ofEpochMilli(exactTime).atZone(zoneId).format(formatter)}")
                }
            }
            if (isEmpty()) {
                add("未设置提醒")
            }
        }

        return TaskDetailUiState(
            taskId = id,
            title = title,
            statusText = when {
                status == TaskStatus.COMPLETED -> "已完成"
                dueAt != null && dueAt < now -> "已逾期"
                else -> "进行中"
            },
            dueText = dueAt?.let {
                Instant.ofEpochMilli(it).atZone(zoneId).format(formatter)
            } ?: "无截止时间",
            progressText = if (subTasks.isEmpty()) {
                "无子任务"
            } else {
                "${subTasks.count { it.isCompleted }}/${subTasks.size} 子任务已完成"
            },
            reminderSummary = reminderLines,
            contentMarkdown = contentMarkdown.orEmpty(),
            isLoading = false,
            emptyMessage = null,
        )
    }
}

private fun Int.toDisplayTime(): String {
    val hour = this / 60
    val minute = this % 60
    return "%02d:%02d".format(hour, minute)
}
