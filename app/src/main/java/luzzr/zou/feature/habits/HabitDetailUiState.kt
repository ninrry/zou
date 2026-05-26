package luzzr.zou.feature.habits

data class HabitDetailUiState(
    val habitId: String? = null,
    val title: String = "习惯详情",
    val frequencyText: String = "",
    val todayStatusText: String = "",
    val modeText: String = "",
    val helperText: String = "",
    val actionMessage: String? = null,
    val contentMarkdown: String = "",
    val steps: List<HabitStepProgressUiModel> = emptyList(),
    val durationInput: String = "",
    val durationTargetText: String = "",
    val durationElapsedSeconds: Long = 0L,
    val durationElapsedText: String = "00:00:00",
    val durationTimerRunning: Boolean = false,
    val durationTimerActionLabel: String = "开始计时",
    val durationCanFinish: Boolean = false,
    val showCheckSection: Boolean = false,
    val showStepsSection: Boolean = false,
    val showDurationSection: Boolean = false,
    val canCheckInToday: Boolean = false,
    val completeButtonLabel: String = "完成今日习惯",
    val completeEnabled: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val isLoading: Boolean = true,
    val emptyMessage: String? = null,
) {
    val canShowContent: Boolean
        get() = contentMarkdown.isNotBlank()

    val shouldShowMainCompleteAction: Boolean
        get() = showCheckSection || showStepsSection
}

data class HabitStepProgressUiModel(
    val id: String,
    val title: String,
    val checked: Boolean = false,
)
