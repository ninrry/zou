package luzzr.zou.feature.today

import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.TaskQuickActionType

data class TodayUiState(
    val title: String = "今日",
    val dateLine: String = "",
    val summary: TodaySummaryUiModel = TodaySummaryUiModel(),
    val tasks: List<TodayTaskCardUiModel> = emptyList(),
    val habits: List<TodayHabitCardUiModel> = emptyList(),
    val recentNotes: List<RecentNoteCardUiModel> = emptyList(),
    val isCompletelyEmpty: Boolean = true,
)

data class TodaySummaryUiModel(
    val pendingTaskCount: Int = 0,
    val dueHabitCount: Int = 0,
    val completedCount: Int = 0,
)

data class TodayTaskCardUiModel(
    val id: String,
    val title: String,
    val actionType: TaskQuickActionType = TaskQuickActionType.NONE,
    val actionLabel: String = "",
    val actionEnabled: Boolean = false,
    val remainingTimeText: String? = null,
    val progressText: String? = null,
    val isCompleted: Boolean = false,
)

data class TodayHabitCardUiModel(
    val id: String,
    val title: String,
    val primaryActionType: HabitQuickActionType = HabitQuickActionType.NONE,
    val primaryActionLabel: String = "",
    val primaryActionEnabled: Boolean = false,
    val secondaryActionType: HabitQuickActionType? = null,
    val secondaryActionLabel: String? = null,
    val secondaryActionEnabled: Boolean = false,
    val progressText: String? = null,
    val statusHint: String? = null,
    val durationRunning: Boolean = false,
    val isCompleted: Boolean = false,
)

data class RecentNoteCardUiModel(
    val id: String,
    val title: String,
    val previewText: String,
    val updatedAtText: String,
)

sealed interface TodayUiEvent {
    data class ShowUndo(
        val message: String,
        val tokenId: String,
    ) : TodayUiEvent

    data class ShowMessage(val message: String) : TodayUiEvent
}
