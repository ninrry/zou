package luzzr.zou.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import luzzr.zou.core.hyperos.XiaomiPowerKeeper
import luzzr.zou.core.reminder.NotificationPermissionChecker
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.usecase.ObserveReminderPreferencesUseCase
import luzzr.zou.domain.usecase.UpdateReminderPreferencesUseCase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    observeReminderPreferencesUseCase: ObserveReminderPreferencesUseCase,
    private val updateReminderPreferencesUseCase: UpdateReminderPreferencesUseCase,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(
        isLoading = true,
        isHyperOS = XiaomiPowerKeeper.isHyperOS(),
    ))
    val uiState = _uiState.asStateFlow()
    private var persistedPreferences: ReminderPreferences? = null

    init {
        viewModelScope.launch {
            refreshOptimizationStatus()

            observeReminderPreferencesUseCase()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaving = false,
                            errorMessage = throwable.message ?: "设置加载失败，请重试。",
                        )
                    }
                }
                .collect { preferences ->
                    persistedPreferences = preferences
                    _uiState.update { current ->
                        current.copy(
                            defaultTaskRepeatIntervalText = preferences.defaultTaskRepeatIntervalMinutes.toString(),
                            defaultHabitRepeatIntervalText = preferences.defaultHabitRepeatIntervalMinutes.toString(),
                            showCompletedTasks = preferences.showCompletedTasks,
                            showOnlyTodayHabits = preferences.showOnlyTodayHabits,
                            showDeletedHabits = preferences.showDeletedHabits,
                            hasPendingChanges = false,
                            notificationPermissionGranted = notificationPermissionChecker.canPostNotifications(),
                            isLoading = false,
                            isSaving = false,
                            defaultsError = null,
                        )
                    }
                }
        }
    }

    fun onHyperOsOptimizationDone() {
        XiaomiPowerKeeper.markOptimizationDone(context)
        _uiState.update { it.copy(hyperOsOptimizationDone = true) }
    }

    /** 刷新优化检测状态，可检测项全部通过时自动关闭引导 */
    fun refreshOptimizationStatus() {
        val isHyperOs = XiaomiPowerKeeper.isHyperOS()
        val status = if (isHyperOs) XiaomiPowerKeeper.checkStatus(context)
            else XiaomiPowerKeeper.OptimizeStatus(true, true, true, true)
        val alreadyDone = !isHyperOs || XiaomiPowerKeeper.isOptimizationDone(context)
        // 可检测项全部通过 → 自动标记完成
        val autoDone = alreadyDone || status.detectableAllOk
        if (autoDone && !alreadyDone) {
            XiaomiPowerKeeper.markOptimizationDone(context)
        }
        _uiState.update {
            it.copy(
                hyperOsOptimizationDone = autoDone || it.hyperOsOptimizationDone,
                optimizeStatus = status,
            )
        }
    }

    fun onTaskDefaultIntervalChanged(value: String) {
        updateEditableState {
            copy(
                defaultTaskRepeatIntervalText = value.filter(Char::isDigit),
            )
        }
    }

    fun onHabitDefaultIntervalChanged(value: String) {
        updateEditableState {
            copy(
                defaultHabitRepeatIntervalText = value.filter(Char::isDigit),
            )
        }
    }

    fun onShowCompletedTasksChanged(checked: Boolean) {
        updateEditableState {
            copy(
                showCompletedTasks = checked,
            )
        }
    }

    fun onShowOnlyTodayHabitsChanged(checked: Boolean) {
        updateEditableState {
            copy(
                showOnlyTodayHabits = checked,
            )
        }
    }

    fun onShowDeletedHabitsChanged(checked: Boolean) {
        updateEditableState {
            copy(
                showDeletedHabits = checked,
            )
        }
    }

    fun saveDefaultIntervals() {
        if (!uiState.value.hasPendingChanges) {
            _uiState.update {
                it.copy(
                    defaultsError = null,
                    errorMessage = null,
                    resultMessage = "当前配置已同步，无需重复保存。",
                )
            }
            return
        }
        val taskMinutes = uiState.value.defaultTaskRepeatIntervalText.toIntOrNull()
        val habitMinutes = uiState.value.defaultHabitRepeatIntervalText.toIntOrNull()
        if (taskMinutes == null || taskMinutes <= 0 || habitMinutes == null || habitMinutes <= 0) {
            _uiState.update {
                it.copy(
                    defaultsError = "默认提醒间隔必须是大于 0 的整数分钟。",
                    errorMessage = null,
                    resultMessage = null,
                )
            }
            return
        }
        updatePreferences(successMessage = "提醒与显示偏好已保存。") {
            it.copy(
                defaultTaskRepeatIntervalMinutes = taskMinutes,
                defaultHabitRepeatIntervalMinutes = habitMinutes,
                showCompletedTasks = uiState.value.showCompletedTasks,
                showOnlyTodayHabits = uiState.value.showOnlyTodayHabits,
                showDeletedHabits = uiState.value.showDeletedHabits,
                settingsUpdatedAt = timeProvider.nowMillis(),
            )
        }
    }

    private fun updateEditableState(transform: SettingsUiState.() -> SettingsUiState) {
        _uiState.update { current ->
            val updatedState = current.transform().copy(
                defaultsError = null,
                errorMessage = null,
                resultMessage = null,
            )
            updatedState.copy(hasPendingChanges = hasPendingChanges(updatedState))
        }
    }

    private fun hasPendingChanges(state: SettingsUiState): Boolean {
        val baseline = persistedPreferences ?: return false
        return state.defaultTaskRepeatIntervalText != baseline.defaultTaskRepeatIntervalMinutes.toString() ||
            state.defaultHabitRepeatIntervalText != baseline.defaultHabitRepeatIntervalMinutes.toString() ||
            state.showCompletedTasks != baseline.showCompletedTasks ||
            state.showOnlyTodayHabits != baseline.showOnlyTodayHabits ||
            state.showDeletedHabits != baseline.showDeletedHabits
    }

    private fun updatePreferences(
        successMessage: String,
        transform: (ReminderPreferences) -> ReminderPreferences,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    resultMessage = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    updateReminderPreferencesUseCase(transform)
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasPendingChanges = false,
                        defaultsError = null,
                        errorMessage = null,
                        resultMessage = successMessage,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "设置保存失败，请重试。",
                    )
                }
            }
        }
    }
}
