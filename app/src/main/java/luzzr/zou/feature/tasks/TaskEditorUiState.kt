package luzzr.zou.feature.tasks

import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority

data class TaskEditorUiState(
    val taskId: String,
    val isEditing: Boolean = false,
    val title: String = "",
    val contentMarkdown: String = "",
    val priority: TaskPriority = TaskPriority.NORMAL,
    val isUrgent: Boolean = false,
    val startReminderMinuteOfDay: Int? = null,
    val windowEndMinuteOfDay: Int? = null,
    val dueAt: Long? = null,
    val repeatIntervalMinutesText: String = "",
    val exactReminderTimes: List<Long> = emptyList(),
    val reminderNotificationTitle: String = "",
    val reminderNotificationBody: String = "",
    val allDay: Boolean = false,
    val completionRule: TaskCompletionRule = TaskCompletionRule.MANUAL,
    val subTasks: List<SubTask> = emptyList(),
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadErrorMessage: String? = null,
    val saveErrorMessage: String? = null,
    val titleError: String? = null,
    val dueAtError: String? = null,
    val repeatIntervalError: String? = null,
    val reminderError: String? = null,
) {
    val screenTitle: String
        get() = if (isEditing) "编辑任务" else "新建任务"

    val saveButtonLabel: String
        get() = if (isEditing) "保存" else "创建"

    val canDelete: Boolean
        get() = isEditing

    val canToggleTaskCompletion: Boolean
        get() = completionRule == TaskCompletionRule.MANUAL || subTasks.isEmpty()

    val hasReminderConfig: Boolean
        get() = startReminderMinuteOfDay != null ||
            repeatIntervalMinutesText.isNotBlank() ||
            exactReminderTimes.isNotEmpty()

    val hasMissingContent: Boolean
        get() = isEditing && !isLoading && loadErrorMessage != null
}
