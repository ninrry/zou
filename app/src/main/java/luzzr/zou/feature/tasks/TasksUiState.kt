package luzzr.zou.feature.tasks

data class TasksUiState(
    val title: String = "待办",
    val showCompleted: Boolean = false,
    val tasks: List<TaskListItemUiModel> = emptyList(),
    val emptyTitle: String = "还没有待办",
    val emptyDescription: String = "点击右下角 + 创建第一条任务，设置截止时间与提醒。",
)

data class TaskListItemUiModel(
    val id: String,
    val title: String,
    val priorityLabel: String,
    val showUrgentBadge: Boolean,
    val dueText: String,
    val progressText: String,
    val isCompleted: Boolean,
    val canToggleCompletion: Boolean,
)
