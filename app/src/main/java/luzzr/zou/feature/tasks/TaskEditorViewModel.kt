package luzzr.zou.feature.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.usecase.GetReminderPreferencesUseCase
import luzzr.zou.domain.usecase.GetTaskUseCase
import luzzr.zou.domain.usecase.SaveTaskUseCase
import luzzr.zou.domain.usecase.SoftDeleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTaskUseCase: GetTaskUseCase,
    private val getReminderPreferencesUseCase: GetReminderPreferencesUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val softDeleteTaskUseCase: SoftDeleteTaskUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val existingTaskId: String? = savedStateHandle[TaskRoutes.taskIdArg]
    private var defaultRepeatIntervalMinutes: Int = 60
    private var originalDueAt: Long? = null
    private var originalExactReminderTimes: Set<Long> = emptySet()

    private val _uiState = MutableStateFlow(
        TaskEditorUiState(
            taskId = existingTaskId ?: UUID.randomUUID().toString(),
            isEditing = existingTaskId != null,
            isLoading = existingTaskId != null,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadDefaults()
        if (existingTaskId != null) {
            loadTask(existingTaskId)
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title, titleError = null, saveErrorMessage = null) }
    }

    fun onContentChanged(content: String) {
        _uiState.update { it.copy(contentMarkdown = content, saveErrorMessage = null) }
    }

    fun onPrioritySelected(priority: TaskPriority) {
        _uiState.update { current ->
            normalizeState(
                current.copy(
                    priority = priority,
                    isUrgent = if (priority == TaskPriority.URGENT) true else current.isUrgent,
                    saveErrorMessage = null,
                ),
            )
        }
    }

    fun onUrgentChanged(isUrgent: Boolean) {
        _uiState.update { current ->
            normalizeState(
                current.copy(
                    isUrgent = isUrgent,
                    priority = if (!isUrgent && current.priority == TaskPriority.URGENT) {
                        TaskPriority.HIGH
                    } else {
                        current.priority
                    },
                    saveErrorMessage = null,
                ),
            )
        }
    }

    fun onCompletionRuleSelected(rule: TaskCompletionRule) {
        _uiState.update { current ->
            normalizeState(current.copy(completionRule = rule, saveErrorMessage = null))
        }
    }

    fun onTaskCompletedChanged(completed: Boolean) {
        _uiState.update { current ->
            if (current.canToggleTaskCompletion) {
                current.copy(isCompleted = completed, saveErrorMessage = null)
            } else {
                current
            }
        }
    }

    fun onDueDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        _uiState.update { current ->
            val zoneId = timeProvider.zoneId()
            val currentDateTime = current.dueAt?.let {
                Instant.ofEpochMilli(it).atZone(zoneId).toLocalDateTime()
            } ?: LocalDateTime.of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(9, 0))
            val updatedDateTime = LocalDateTime.of(
                LocalDate.of(year, month, dayOfMonth),
                currentDateTime.toLocalTime(),
            )
            current.copy(
                dueAt = updatedDateTime.atZone(zoneId).toInstant().toEpochMilli(),
                dueAtError = null,
                reminderError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onDueTimeSelected(hour: Int, minute: Int) {
        _uiState.update { current ->
            val zoneId = timeProvider.zoneId()
            val currentDate = current.dueAt?.let {
                Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
            } ?: LocalDate.now(zoneId)
            val updatedDateTime = LocalDateTime.of(currentDate, LocalTime.of(hour, minute))
            current.copy(
                dueAt = updatedDateTime.atZone(zoneId).toInstant().toEpochMilli(),
                dueAtError = null,
                reminderError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onDueCleared() {
        _uiState.update { it.copy(dueAt = null, dueAtError = null, reminderError = null, saveErrorMessage = null) }
    }

    fun onStartReminderMinuteChanged(minuteOfDay: Int?) {
        _uiState.update { current ->
            current.copy(
                startReminderMinuteOfDay = minuteOfDay,
                repeatIntervalMinutesText = if (minuteOfDay != null && current.repeatIntervalMinutesText.isBlank()) {
                    defaultRepeatIntervalMinutes.toString()
                } else {
                    current.repeatIntervalMinutesText
                },
                reminderError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onReminderNotificationTitleChanged(value: String) {
        _uiState.update { it.copy(reminderNotificationTitle = value, saveErrorMessage = null) }
    }

    fun onReminderNotificationBodyChanged(value: String) {
        _uiState.update { it.copy(reminderNotificationBody = value, saveErrorMessage = null) }
    }

    fun onWindowEndMinuteChanged(minuteOfDay: Int?) {
        _uiState.update { it.copy(windowEndMinuteOfDay = minuteOfDay, reminderError = null, saveErrorMessage = null) }
    }

    fun onReminderActiveFromChanged(epochMillis: Long?) {
        _uiState.update {
            it.copy(
                reminderActiveFrom = epochMillis,
                reminderDateRangeError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onReminderActiveToChanged(epochMillis: Long?) {
        _uiState.update {
            it.copy(
                reminderActiveTo = epochMillis,
                reminderDateRangeError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onRepeatIntervalChanged(value: String) {
        _uiState.update {
            it.copy(
                repeatIntervalMinutesText = value.filter(Char::isDigit),
                repeatIntervalError = null,
                reminderError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onAddExactReminder(remindAt: Long) {
        _uiState.update { current ->
            current.copy(
                exactReminderTimes = (current.exactReminderTimes + remindAt).distinct().sorted(),
                reminderError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onRemoveExactReminder(remindAt: Long) {
        _uiState.update { current ->
            current.copy(
                exactReminderTimes = current.exactReminderTimes.filterNot { it == remindAt },
                saveErrorMessage = null,
            )
        }
    }

    fun onAddSubTask() {
        _uiState.update { current ->
            current.copy(
                subTasks = current.subTasks + SubTask(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    sortOrder = current.subTasks.size,
                ),
                saveErrorMessage = null,
            )
        }
    }

    fun onSubTaskTitleChanged(subTaskId: String, title: String) {
        _uiState.update { current ->
            normalizeState(
                current.copy(
                    subTasks = current.subTasks.map { subTask ->
                        if (subTask.id == subTaskId) subTask.copy(title = title) else subTask
                    },
                    saveErrorMessage = null,
                ),
            )
        }
    }

    fun onSubTaskCompletedChanged(subTaskId: String, completed: Boolean) {
        _uiState.update { current ->
            normalizeState(
                current.copy(
                    subTasks = current.subTasks.map { subTask ->
                        if (subTask.id == subTaskId) {
                            subTask.copy(
                                isCompleted = completed,
                                completedAt = if (completed) timeProvider.nowMillis() else null,
                            )
                        } else {
                            subTask
                        }
                    },
                    saveErrorMessage = null,
                ),
            )
        }
    }

    fun onRemoveSubTask(subTaskId: String) {
        _uiState.update { current ->
            normalizeState(
                current.copy(
                    subTasks = current.subTasks
                        .filterNot { it.id == subTaskId }
                        .mapIndexed { index, subTask -> subTask.copy(sortOrder = index) },
                    saveErrorMessage = null,
                ),
            )
        }
    }

    fun validateBeforeSave(): Boolean {
        val currentState = uiState.value
        if (currentState.hasMissingContent) {
            _uiState.update { it.copy(saveErrorMessage = currentState.loadErrorMessage) }
            return false
        }
        if (currentState.title.trim().isEmpty()) {
            _uiState.update { it.copy(titleError = "标题不能为空", saveErrorMessage = "标题不能为空") }
            return false
        }
        if (!validateDueAt(currentState)) {
            return false
        }

        val repeatIntervalMinutes = currentState.repeatIntervalMinutesText.toIntOrNull()
        if (currentState.repeatIntervalMinutesText.isNotBlank() && repeatIntervalMinutes == null) {
            _uiState.update {
                it.copy(
                    repeatIntervalError = "重复提醒间隔必须是正整数",
                    saveErrorMessage = "重复提醒间隔必须是正整数",
                )
            }
            return false
        }
        if (repeatIntervalMinutes != null && repeatIntervalMinutes <= 0) {
            _uiState.update {
                it.copy(
                    repeatIntervalError = "重复提醒间隔必须大于 0",
                    saveErrorMessage = "重复提醒间隔必须大于 0",
                )
            }
            return false
        }
        if (currentState.windowEndMinuteOfDay != null && currentState.startReminderMinuteOfDay == null) {
            _uiState.update {
                it.copy(
                    reminderError = "请先设置开始提醒时间",
                    saveErrorMessage = "请先设置开始提醒时间",
                )
            }
            return false
        }
        if (
            currentState.startReminderMinuteOfDay != null &&
            currentState.windowEndMinuteOfDay != null &&
            currentState.windowEndMinuteOfDay < currentState.startReminderMinuteOfDay
        ) {
            _uiState.update {
                it.copy(
                    reminderError = "提醒窗口结束时间不能早于开始提醒时间",
                    saveErrorMessage = "提醒窗口结束时间不能早于开始提醒时间",
                )
            }
            return false
        }
        if (repeatIntervalMinutes != null && currentState.startReminderMinuteOfDay == null) {
            _uiState.update {
                it.copy(
                    reminderError = "开启重复提醒前需要先设置开始提醒时间",
                    saveErrorMessage = "开启重复提醒前需要先设置开始提醒时间",
                )
            }
            return false
        }
        if (repeatIntervalMinutes != null && currentState.windowEndMinuteOfDay == null) {
            _uiState.update {
                it.copy(
                    reminderError = "重复提醒需要设置窗口结束时间",
                    saveErrorMessage = "重复提醒需要设置窗口结束时间",
                )
            }
            return false
        }

        // 提醒日期范围校验
        val activeFrom = currentState.reminderActiveFrom
        val activeTo = currentState.reminderActiveTo
        if (activeFrom != null && activeTo != null && activeTo < activeFrom) {
            _uiState.update {
                it.copy(
                    reminderDateRangeError = "提醒结束日期不能早于开始日期",
                    saveErrorMessage = "提醒结束日期不能早于开始日期",
                )
            }
            return false
        }

        _uiState.update {
            it.copy(
                titleError = null,
                dueAtError = null,
                repeatIntervalError = null,
                reminderError = null,
                reminderDateRangeError = null,
                saveErrorMessage = null,
            )
        }
        return true
    }

    fun saveTask(onSaved: () -> Unit = {}) {
        if (_uiState.value.isSaving || !validateBeforeSave()) return
        val currentState = uiState.value
        val trimmedTitle = currentState.title.trim()

        _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }
        viewModelScope.launch {
            val cleanedSubTasks = currentState.subTasks
                .filter { it.title.isNotBlank() }
                .mapIndexed { index, subTask -> subTask.copy(sortOrder = index) }
            val normalizedState = normalizeState(currentState.copy(subTasks = cleanedSubTasks))
            runCatching {
                saveTaskUseCase(
                    task = Task(
                        id = normalizedState.taskId,
                        title = trimmedTitle,
                        contentMarkdown = normalizedState.contentMarkdown.ifBlank { null },
                        priority = normalizedState.priority,
                        isUrgent = normalizedState.isUrgent,
                        status = if (normalizedState.isCompleted) TaskStatus.COMPLETED else TaskStatus.ACTIVE,
                        startReminderMinuteOfDay = normalizedState.startReminderMinuteOfDay,
                        windowEndMinuteOfDay = normalizedState.windowEndMinuteOfDay,
                        dueAt = normalizedState.dueAt,
                        repeatIntervalMinutes = normalizedState.repeatIntervalMinutesText.toIntOrNull(),
                        exactReminderTimes = normalizedState.exactReminderTimes.distinct().sorted(),
                        reminderNotificationTitle = normalizedState.reminderNotificationTitle.trim().ifBlank { null },
                        reminderNotificationBody = normalizedState.reminderNotificationBody.trim().ifBlank { null },
                        allDay = normalizedState.allDay,
                        completionRule = normalizedState.completionRule,
                        createdAt = normalizedState.createdAt,
                        subTasks = cleanedSubTasks,
                        startRemindAt = normalizedState.reminderActiveFrom,
                        remindWindowEndAt = normalizedState.reminderActiveTo,
                    ),
                    subTasks = cleanedSubTasks,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveErrorMessage = null) }
                onSaved()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = throwable.message ?: "保存任务失败，请重试。",
                    )
                }
            }
        }
    }

    fun onDeleteClicked(onDeleted: () -> Unit = {}): Boolean {
        if (uiState.value.isSaving || !uiState.value.isEditing || uiState.value.hasMissingContent) return false
        val taskId = uiState.value.taskId
        viewModelScope.launch {
            runCatching {
                softDeleteTaskUseCase(taskId)
            }.onSuccess {
                _uiState.update { it.copy(saveErrorMessage = null) }
                onDeleted()
            }.onFailure { throwable ->
                _uiState.update { it.copy(saveErrorMessage = throwable.message ?: "删除任务失败，请重试。") }
            }
        }
        return true
    }

    private fun loadTask(taskId: String) {
        viewModelScope.launch {
            val task = getTaskUseCase(taskId)
            _uiState.update { current ->
                if (task == null) {
                    current.copy(isLoading = false, loadErrorMessage = "任务不存在或已删除。")
                } else {
                    TaskEditorUiState(
                        taskId = task.id,
                        isEditing = true,
                        title = task.title,
                        contentMarkdown = task.contentMarkdown.orEmpty(),
                        priority = task.priority,
                        isUrgent = task.isUrgent,
                        startReminderMinuteOfDay = task.startReminderMinuteOfDay,
                        windowEndMinuteOfDay = task.windowEndMinuteOfDay,
                        dueAt = task.dueAt,
                        repeatIntervalMinutesText = task.repeatIntervalMinutes?.toString().orEmpty(),
                        exactReminderTimes = task.exactReminderTimes,
                        reminderNotificationTitle = task.reminderNotificationTitle.orEmpty(),
                        reminderNotificationBody = task.reminderNotificationBody.orEmpty(),
                        allDay = task.allDay,
                        completionRule = task.completionRule,
                        subTasks = task.subTasks,
                        isCompleted = task.status == TaskStatus.COMPLETED,
                        createdAt = task.createdAt,
                        reminderActiveFrom = task.startRemindAt,
                        reminderActiveTo = task.remindWindowEndAt,
                        isLoading = false,
                        loadErrorMessage = null,
                    )
                }
            }
            task?.let {
                originalDueAt = it.dueAt
                originalExactReminderTimes = it.exactReminderTimes.toSet()
            }
        }
    }

    private fun loadDefaults() {
        if (existingTaskId != null) return
        viewModelScope.launch {
            defaultRepeatIntervalMinutes = getReminderPreferencesUseCase().defaultTaskRepeatIntervalMinutes
        }
    }

    private fun normalizeState(state: TaskEditorUiState): TaskEditorUiState {
        return if (state.completionRule == TaskCompletionRule.AUTO_ALL_SUBTASKS && state.subTasks.isNotEmpty()) {
            state.copy(isCompleted = state.subTasks.all { it.isCompleted })
        } else {
            state
        }
    }

    private fun validateDueAt(state: TaskEditorUiState): Boolean {
        val now = timeProvider.nowMillis()
        if (isNewOrChangedFutureFieldInvalid(state.dueAt, originalDueAt, now)) {
            _uiState.update { it.copy(dueAtError = "截止时间不能早于当前时间", saveErrorMessage = "截止时间不能早于当前时间") }
            return false
        }
        val addedExactReminders = state.exactReminderTimes.filterNot(originalExactReminderTimes::contains)
        if (addedExactReminders.any { it < now }) {
            _uiState.update { it.copy(reminderError = "特别提醒时间不能早于当前时间", saveErrorMessage = "特别提醒时间不能早于当前时间") }
            return false
        }
        return true
    }

    private fun isNewOrChangedFutureFieldInvalid(
        currentValue: Long?,
        originalValue: Long?,
        now: Long,
    ): Boolean {
        if (currentValue == null || currentValue >= now) return false
        return !uiState.value.isEditing || currentValue != originalValue
    }
}
