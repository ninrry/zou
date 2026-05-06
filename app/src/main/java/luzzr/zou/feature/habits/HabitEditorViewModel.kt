package luzzr.zou.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitStep
import luzzr.zou.domain.usecase.GetHabitUseCase
import luzzr.zou.domain.usecase.GetReminderPreferencesUseCase
import luzzr.zou.domain.usecase.SaveHabitUseCase
import luzzr.zou.domain.usecase.SoftDeleteHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHabitUseCase: GetHabitUseCase,
    private val getReminderPreferencesUseCase: GetReminderPreferencesUseCase,
    private val saveHabitUseCase: SaveHabitUseCase,
    private val softDeleteHabitUseCase: SoftDeleteHabitUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val existingHabitId: String? = savedStateHandle[HabitRoutes.habitIdArg]
    private var defaultRepeatIntervalMinutes: Int = 60
    private var originalExactReminderTimes: Set<LocalTime> = emptySet()

    private val _uiState = MutableStateFlow(
        HabitEditorUiState(
            habitId = existingHabitId ?: UUID.randomUUID().toString(),
            isEditing = existingHabitId != null,
            isLoading = existingHabitId != null,
            intervalAnchorDate = currentDate(),
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadDefaults()
        if (existingHabitId != null) {
            loadHabit(existingHabitId)
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title, titleError = null, saveErrorMessage = null) }
    }

    fun onContentChanged(content: String) {
        _uiState.update { it.copy(contentMarkdown = content, saveErrorMessage = null) }
    }

    fun onFrequencySelected(frequencyType: HabitFrequencyType) {
        _uiState.update {
            it.copy(frequencyType = frequencyType, frequencyError = null, saveErrorMessage = null)
        }
    }

    fun onWeekdayToggled(dayOfWeek: java.time.DayOfWeek) {
        _uiState.update { current ->
            val updatedWeekdays = current.selectedWeekdays.toMutableSet()
            if (!updatedWeekdays.add(dayOfWeek)) {
                updatedWeekdays.remove(dayOfWeek)
            }
            current.copy(
                selectedWeekdays = updatedWeekdays,
                frequencyError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onIntervalDaysChanged(value: String) {
        _uiState.update {
            it.copy(
                intervalDaysText = value.filter(Char::isDigit),
                frequencyError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onIntervalAnchorDateChanged(date: LocalDate) {
        _uiState.update {
            it.copy(intervalAnchorDate = date, frequencyError = null, saveErrorMessage = null)
        }
    }

    fun onMonthlyDaysChanged(value: String) {
        _uiState.update {
            it.copy(monthlyDaysText = value, frequencyError = null, saveErrorMessage = null)
        }
    }

    fun onRemindWindowStartChanged(value: LocalTime?) {
        _uiState.update { current ->
            current.copy(
                remindWindowStart = value,
                repeatIntervalMinutesText = if (value != null && current.repeatIntervalMinutesText.isBlank()) {
                    defaultRepeatIntervalMinutes.toString()
                } else {
                    current.repeatIntervalMinutesText
                },
                reminderError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onRemindWindowEndChanged(value: LocalTime?) {
        _uiState.update { it.copy(remindWindowEnd = value, reminderError = null, saveErrorMessage = null) }
    }

    fun onRepeatIntervalChanged(value: String) {
        _uiState.update {
            it.copy(
                repeatIntervalMinutesText = value.filter(Char::isDigit),
                reminderError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onAddExactReminder(value: LocalTime) {
        _uiState.update { current ->
            current.copy(
                exactReminderTimes = (current.exactReminderTimes + value).distinct().sorted(),
                reminderError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onRemoveExactReminder(value: LocalTime) {
        _uiState.update {
            it.copy(
                exactReminderTimes = it.exactReminderTimes.filterNot { time -> time == value },
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

    fun onCheckInModeSelected(mode: HabitCheckInMode) {
        _uiState.update {
            it.copy(
                checkInMode = mode,
                targetDurationError = null,
                stepsError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onTargetDurationChanged(value: String) {
        _uiState.update {
            it.copy(
                targetDurationText = value.filter(Char::isDigit),
                targetDurationError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onAddStep() {
        _uiState.update { current ->
            current.copy(
                steps = current.steps + HabitStep(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    sortOrder = current.steps.size,
                ),
                stepsError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onStepTitleChanged(stepId: String, title: String) {
        _uiState.update { current ->
            current.copy(
                steps = current.steps.map { step ->
                    if (step.id == stepId) step.copy(title = title) else step
                },
                stepsError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onRemoveStep(stepId: String) {
        _uiState.update { current ->
            current.copy(
                steps = current.steps
                    .filterNot { it.id == stepId }
                    .mapIndexed { index, step -> step.copy(sortOrder = index) },
                stepsError = null,
                saveErrorMessage = null,
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

        when (currentState.frequencyType) {
            HabitFrequencyType.DAILY -> Unit
            HabitFrequencyType.WEEKLY -> {
                if (currentState.selectedWeekdays.isEmpty()) {
                    _uiState.update { it.copy(frequencyError = "请至少选择一个执行日", saveErrorMessage = "请至少选择一个执行日") }
                    return false
                }
            }
            HabitFrequencyType.INTERVAL_DAYS -> {
                val intervalDays = currentState.intervalDaysText.toIntOrNull()
                if (intervalDays == null || intervalDays <= 0) {
                    _uiState.update { it.copy(frequencyError = "间隔天数必须大于 0", saveErrorMessage = "间隔天数必须大于 0") }
                    return false
                }
                if (currentState.intervalAnchorDate == null) {
                    _uiState.update { it.copy(frequencyError = "请设置起始日期", saveErrorMessage = "请设置起始日期") }
                    return false
                }
            }
            HabitFrequencyType.MONTHLY -> {
                if (parseMonthlyDays(currentState.monthlyDaysText) == null) {
                    val message = "请输入有效的每月日期，例如 1,15,31"
                    _uiState.update { it.copy(frequencyError = message, saveErrorMessage = message) }
                    return false
                }
            }
        }

        when (currentState.checkInMode) {
            HabitCheckInMode.CHECK -> Unit
            HabitCheckInMode.STEPS -> {
                if (currentState.steps.none { it.title.isNotBlank() }) {
                    val message = "步骤型习惯至少需要一个步骤"
                    _uiState.update { it.copy(stepsError = message, saveErrorMessage = message) }
                    return false
                }
            }
            HabitCheckInMode.DURATION -> {
                val targetDuration = currentState.targetDurationText.toIntOrNull()
                if (targetDuration == null || targetDuration <= 0) {
                    val message = "目标时长必须大于 0 分钟"
                    _uiState.update { it.copy(targetDurationError = message, saveErrorMessage = message) }
                    return false
                }
            }
        }

        val repeatIntervalMinutes = currentState.repeatIntervalMinutesText.toIntOrNull()
        if (repeatIntervalMinutes != null && repeatIntervalMinutes <= 0) {
            val message = "重复提醒间隔必须大于 0"
            _uiState.update { it.copy(reminderError = message, saveErrorMessage = message) }
            return false
        }
        if (currentState.remindWindowEnd != null && currentState.remindWindowStart == null) {
            val message = "请先设置提醒窗口开始时间"
            _uiState.update { it.copy(reminderError = message, saveErrorMessage = message) }
            return false
        }
        if (repeatIntervalMinutes != null && currentState.remindWindowStart == null) {
            val message = "设置重复提醒前需要先设置提醒窗口开始时间"
            _uiState.update { it.copy(reminderError = message, saveErrorMessage = message) }
            return false
        }
        if (repeatIntervalMinutes != null && currentState.remindWindowEnd == null) {
            val message = "设置重复提醒前需要先设置提醒窗口结束时间"
            _uiState.update { it.copy(reminderError = message, saveErrorMessage = message) }
            return false
        }
        if (
            currentState.remindWindowStart != null &&
            currentState.remindWindowEnd != null &&
            currentState.remindWindowEnd <= currentState.remindWindowStart
        ) {
            val message = "提醒窗口暂不支持跨日，结束时间必须晚于开始时间"
            _uiState.update { it.copy(reminderError = message, saveErrorMessage = message) }
            return false
        }
        if (!validateExactReminderTimes(currentState)) {
            return false
        }

        _uiState.update {
            it.copy(
                titleError = null,
                frequencyError = null,
                targetDurationError = null,
                stepsError = null,
                reminderError = null,
                saveErrorMessage = null,
            )
        }
        return true
    }

    fun saveHabit(onSaved: () -> Unit = {}) {
        if (_uiState.value.isSaving || !validateBeforeSave()) return

        val currentState = uiState.value
        val now = timeProvider.nowMillis()
        val cleanedSteps = if (currentState.checkInMode == HabitCheckInMode.STEPS) {
            currentState.steps
                .filter { it.title.isNotBlank() }
                .mapIndexed { index, step ->
                    step.copy(
                        sortOrder = index,
                        createdAt = if (step.createdAt == 0L) now else step.createdAt,
                        updatedAt = now,
                    )
                }
        } else {
            emptyList()
        }

        _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }
        viewModelScope.launch {
            runCatching {
                saveHabitUseCase(
                    habit = Habit(
                        id = currentState.habitId,
                        title = currentState.title.trim(),
                        contentMarkdown = currentState.contentMarkdown.ifBlank { null },
                        frequencyType = currentState.frequencyType,
                        selectedWeekdays = if (currentState.frequencyType == HabitFrequencyType.WEEKLY) {
                            currentState.selectedWeekdays
                        } else {
                            emptySet()
                        },
                        intervalDays = if (currentState.frequencyType == HabitFrequencyType.INTERVAL_DAYS) {
                            currentState.intervalDaysText.toIntOrNull()
                        } else {
                            null
                        },
                        intervalAnchorDate = if (currentState.frequencyType == HabitFrequencyType.INTERVAL_DAYS) {
                            currentState.intervalAnchorDate
                        } else {
                            null
                        },
                        monthlyDays = if (currentState.frequencyType == HabitFrequencyType.MONTHLY) {
                            parseMonthlyDays(currentState.monthlyDaysText).orEmpty()
                        } else {
                            emptySet()
                        },
                        remindWindowStart = currentState.remindWindowStart,
                        remindWindowEnd = currentState.remindWindowEnd,
                        repeatIntervalMinutes = currentState.repeatIntervalMinutesText.toIntOrNull(),
                        exactReminderTimes = currentState.exactReminderTimes,
                        reminderNotificationTitle = currentState.reminderNotificationTitle.trim().ifBlank { null },
                        reminderNotificationBody = currentState.reminderNotificationBody.trim().ifBlank { null },
                        checkInMode = currentState.checkInMode,
                        targetDurationMinutes = if (currentState.checkInMode == HabitCheckInMode.DURATION) {
                            currentState.targetDurationText.toIntOrNull()
                        } else {
                            null
                        },
                        createdAt = currentState.createdAt,
                        steps = cleanedSteps,
                    ),
                    steps = cleanedSteps,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveErrorMessage = null) }
                onSaved()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = throwable.message ?: "保存习惯失败，请重试。",
                    )
                }
            }
        }
    }

    fun onDeleteClicked(onDeleted: () -> Unit = {}) {
        if (uiState.value.isSaving || !uiState.value.isEditing || uiState.value.hasMissingContent) return
        viewModelScope.launch {
            runCatching { softDeleteHabitUseCase(uiState.value.habitId) }
                .onSuccess {
                    _uiState.update { it.copy(saveErrorMessage = null) }
                    onDeleted()
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(saveErrorMessage = throwable.message ?: "删除习惯失败，请重试。") }
                }
        }
    }

    private fun loadHabit(habitId: String) {
        viewModelScope.launch {
            val habit = getHabitUseCase(habitId = habitId, recordDate = currentDate().toEpochDay())
            _uiState.update { current ->
                if (habit == null) {
                    current.copy(
                        isLoading = false,
                        loadErrorMessage = "习惯不存在或已删除。",
                    )
                } else {
                    HabitEditorUiState(
                        habitId = habit.id,
                        isEditing = true,
                        title = habit.title,
                        contentMarkdown = habit.contentMarkdown.orEmpty(),
                        frequencyType = habit.frequencyType,
                        selectedWeekdays = habit.selectedWeekdays,
                        intervalDaysText = habit.intervalDays?.toString().orEmpty(),
                        intervalAnchorDate = habit.intervalAnchorDate ?: currentDate(),
                        monthlyDaysText = habit.monthlyDays.sorted().joinToString(","),
                        remindWindowStart = habit.remindWindowStart,
                        remindWindowEnd = habit.remindWindowEnd,
                        repeatIntervalMinutesText = habit.repeatIntervalMinutes?.toString().orEmpty(),
                        exactReminderTimes = habit.exactReminderTimes,
                        reminderNotificationTitle = habit.reminderNotificationTitle.orEmpty(),
                        reminderNotificationBody = habit.reminderNotificationBody.orEmpty(),
                        checkInMode = habit.checkInMode,
                        targetDurationText = habit.targetDurationMinutes?.toString().orEmpty(),
                        steps = habit.steps,
                        createdAt = habit.createdAt,
                        isLoading = false,
                        loadErrorMessage = null,
                    )
                }
            }
            habit?.let { originalExactReminderTimes = it.exactReminderTimes.toSet() }
        }
    }

    private fun parseMonthlyDays(value: String): Set<Int>? {
        if (value.isBlank()) return null
        val tokens = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null

        val parsedDays = mutableSetOf<Int>()
        tokens.forEach { token ->
            val day = token.toIntOrNull() ?: return null
            if (day !in 1..31) return null
            parsedDays += day
        }
        return parsedDays
    }

    private fun currentDate(): LocalDate {
        return Instant.ofEpochMilli(timeProvider.nowMillis())
            .atZone(timeProvider.zoneId())
            .toLocalDate()
    }

    private fun loadDefaults() {
        if (existingHabitId != null) return
        viewModelScope.launch {
            defaultRepeatIntervalMinutes = getReminderPreferencesUseCase().defaultHabitRepeatIntervalMinutes
        }
    }

    private fun validateExactReminderTimes(state: HabitEditorUiState): Boolean {
        val addedTimes = state.exactReminderTimes.filterNot(originalExactReminderTimes::contains)
        if (addedTimes.isEmpty()) return true
        if (!isDueToday(state, currentDate())) return true
        val nowTime = Instant.ofEpochMilli(timeProvider.nowMillis())
            .atZone(timeProvider.zoneId())
            .toLocalTime()
        if (addedTimes.any { it < nowTime }) {
            val message = "今天的特别提醒时间不能早于当前时间"
            _uiState.update { it.copy(reminderError = message, saveErrorMessage = message) }
            return false
        }
        return true
    }

    private fun isDueToday(state: HabitEditorUiState, today: LocalDate): Boolean {
        return when (state.frequencyType) {
            HabitFrequencyType.DAILY -> true
            HabitFrequencyType.WEEKLY -> state.selectedWeekdays.contains(today.dayOfWeek)
            HabitFrequencyType.INTERVAL_DAYS -> {
                val intervalDays = state.intervalDaysText.toIntOrNull() ?: return false
                val anchorDate = state.intervalAnchorDate ?: return false
                if (today.isBefore(anchorDate)) return false
                ChronoUnit.DAYS.between(anchorDate, today) % intervalDays == 0L
            }
            HabitFrequencyType.MONTHLY -> parseMonthlyDays(state.monthlyDaysText)?.contains(today.dayOfMonth) == true
        }
    }
}
