package luzzr.zou.feature.tasks

data class TaskDetailUiState(
    val taskId: String? = null,
    val title: String = "任务详情",
    val statusText: String = "",
    val dueText: String = "无截止时间",
    val progressText: String = "无子任务",
    val reminderSummary: List<String> = emptyList(),
    val contentMarkdown: String = "",
    val isLoading: Boolean = true,
    val emptyMessage: String? = null,
) {
    val canEdit: Boolean
        get() = taskId != null && emptyMessage == null
}
