package luzzr.zou.feature.settings

data class SettingsUiState(
    val title: String = "设置",
    val defaultTaskRepeatIntervalText: String = "",
    val defaultHabitRepeatIntervalText: String = "",
    val showCompletedTasks: Boolean = false,
    val showOnlyTodayHabits: Boolean = false,
    val showDeletedHabits: Boolean = false,
    val hasPendingChanges: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val defaultsError: String? = null,
    val errorMessage: String? = null,
    val resultMessage: String? = null,
)
