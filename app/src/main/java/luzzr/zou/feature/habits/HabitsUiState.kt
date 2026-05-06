package luzzr.zou.feature.habits

import luzzr.zou.domain.model.HabitTodayStatus
import luzzr.zou.domain.usecase.HabitQuickActionType

data class HabitsUiState(
    val title: String = "习惯",
    val todayOnly: Boolean = false,
    val showDeleted: Boolean = false,
    val activeHabits: List<HabitCardUiModel> = emptyList(),
    val deletedHabits: List<HabitCardUiModel> = emptyList(),
    val emptyTitle: String = "还没有习惯",
    val emptyDescription: String = "点击右下角创建你的第一个习惯。",
)

data class HabitCardUiModel(
    val id: String,
    val title: String,
    val frequencyText: String,
    val modeText: String,
    val statusText: String,
    val supportingText: String,
    val todayStatus: HabitTodayStatus,
    val isDeleted: Boolean,
    val canQuickCheck: Boolean,
    val quickActionLabel: String? = null,
    val quickActionType: HabitQuickActionType = HabitQuickActionType.NONE,
    val quickActionEnabled: Boolean = false,
    val canOpenDetail: Boolean,
    val canRestore: Boolean,
    val updatedAt: Long,
)
