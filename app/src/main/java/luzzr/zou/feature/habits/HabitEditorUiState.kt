package luzzr.zou.feature.habits

import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitStep
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class HabitEditorUiState(
    val habitId: String,
    val isEditing: Boolean = false,
    val title: String = "",
    val contentMarkdown: String = "",
    val frequencyType: HabitFrequencyType = HabitFrequencyType.DAILY,
    val selectedWeekdays: Set<DayOfWeek> = emptySet(),
    val intervalDaysText: String = "",
    val intervalAnchorDate: LocalDate? = null,
    val monthlyDaysText: String = "",
    val remindWindowStart: LocalTime? = null,
    val remindWindowEnd: LocalTime? = null,
    val repeatIntervalMinutesText: String = "",
    val exactReminderTimes: List<LocalTime> = emptyList(),
    val reminderNotificationTitle: String = "",
    val reminderNotificationBody: String = "",
    val checkInMode: HabitCheckInMode = HabitCheckInMode.CHECK,
    val targetDurationText: String = "",
    val steps: List<HabitStep> = emptyList(),
    val createdAt: Long = 0L,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadErrorMessage: String? = null,
    val saveErrorMessage: String? = null,
    val titleError: String? = null,
    val frequencyError: String? = null,
    val targetDurationError: String? = null,
    val stepsError: String? = null,
    val reminderError: String? = null,
) {
    val screenTitle: String
        get() = if (isEditing) "编辑习惯" else "新建习惯"

    val saveButtonLabel: String
        get() = if (isEditing) "保存" else "创建"

    val canDelete: Boolean
        get() = isEditing

    val hasReminderConfig: Boolean
        get() = remindWindowStart != null || exactReminderTimes.isNotEmpty()

    val hasMissingContent: Boolean
        get() = isEditing && !isLoading && loadErrorMessage != null
}
