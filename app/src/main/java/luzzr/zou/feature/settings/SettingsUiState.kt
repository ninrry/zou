package luzzr.zou.feature.settings

import luzzr.zou.core.hyperos.XiaomiPowerKeeper

data class SettingsUiState(
    val title: String = "设置",
    val defaultTaskRepeatIntervalText: String = "",
    val defaultHabitRepeatIntervalText: String = "",
    val showCompletedTasks: Boolean = false,
    val showOnlyTodayHabits: Boolean = false,
    val showDeletedHabits: Boolean = false,
    val defaultStartDestination: String = "today",
    val hasPendingChanges: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val isHyperOS: Boolean = false,
    val hyperOsOptimizationDone: Boolean = false,
    val optimizeStatus: XiaomiPowerKeeper.OptimizeStatus = XiaomiPowerKeeper.OptimizeStatus(
        batteryOptOk = false,
        exactAlarmOk = false,
        autoStartOk = false,
        lockScreenOk = false,
    ),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val defaultsError: String? = null,
    val errorMessage: String? = null,
    val resultMessage: String? = null,
)
