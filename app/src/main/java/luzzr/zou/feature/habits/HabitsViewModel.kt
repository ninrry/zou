package luzzr.zou.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitTodayStatus
import luzzr.zou.domain.usecase.CheckInPolicy
import luzzr.zou.domain.usecase.CompleteCheckHabitUseCase
import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.HabitScheduleEvaluator
import luzzr.zou.domain.usecase.ObserveReminderPreferencesUseCase
import luzzr.zou.domain.usecase.ObserveHabitsUseCase
import luzzr.zou.domain.usecase.RestoreHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel @Inject constructor(
    observeReminderPreferencesUseCase: ObserveReminderPreferencesUseCase,
    private val observeHabitsUseCase: ObserveHabitsUseCase,
    private val completeCheckHabitUseCase: CompleteCheckHabitUseCase,
    private val restoreHabitUseCase: RestoreHabitUseCase,
    private val habitScheduleEvaluator: HabitScheduleEvaluator,
    private val checkInPolicy: CheckInPolicy,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    val uiState: StateFlow<HabitsUiState> = observeReminderPreferencesUseCase()
        .map { preferences -> preferences.showOnlyTodayHabits to preferences.showDeletedHabits }
        .flatMapLatest { (onlyToday, deleted) ->
            observeHabitsUseCase(
                recordDate = todayEpochDay(),
                includeDeleted = deleted,
            ).map { habits ->
                val today = currentDate()
                val activeItems = habits
                    .filterNot { it.isDeleted }
                    .filter { habit -> !onlyToday || habitScheduleEvaluator.isDueOn(habit, today) }
                    .map { habit -> habit.toCardUiModel(today) }
                    .sortedWith(
                        compareBy<HabitCardUiModel>(::sortBucket)
                            .thenByDescending { it.updatedAt },
                    )

                val deletedItems = if (deleted) {
                    habits
                        .filter { it.isDeleted }
                        .map { habit -> habit.toCardUiModel(today) }
                        .sortedByDescending { it.updatedAt }
                } else {
                    emptyList()
                }

                HabitsUiState(
                    todayOnly = onlyToday,
                    showDeleted = deleted,
                    activeHabits = activeItems,
                    deletedHabits = deletedItems,
                    emptyTitle = if (onlyToday) {
                        "今天没有需要执行的习惯"
                    } else {
                        "还没有习惯"
                    },
                    emptyDescription = if (onlyToday) {
                        "今天的习惯已完成，或今天没有命中执行频率。"
                    } else {
                        "点击右下角 + 创建一个习惯，设定频率和打卡方式。"
                    },
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HabitsUiState(),
        )

    fun onQuickCheckHabit(habitId: String) {
        viewModelScope.launch {
            completeCheckHabitUseCase(
                habitId = habitId,
                recordDate = todayEpochDay(),
            )
        }
    }

    fun onRestoreHabit(habitId: String) {
        viewModelScope.launch {
            restoreHabitUseCase(habitId)
        }
    }

    private fun Habit.toCardUiModel(today: java.time.LocalDate): HabitCardUiModel {
        val todayStatus = habitScheduleEvaluator.getTodayStatus(
            habit = this,
            date = today,
            todayRecord = todayRecord,
        )
        val quickState = checkInPolicy.evaluateHabitQuickAction(
            habit = this,
            todayStatus = todayStatus,
            nowMillis = timeProvider.nowMillis(),
        )
        val canQuickCheck = quickState.primaryActionType == HabitQuickActionType.CHECK
        val quickActionEnabled = quickState.primaryActionEnabled
        val quickActionLabel = when {
            isDeleted -> null
            canQuickCheck -> quickState.primaryActionLabel
            todayStatus == HabitTodayStatus.DUE -> if (quickActionEnabled) "去完成" else "未到可打卡时间"
            else -> null
        }
        return HabitCardUiModel(
            id = id,
            title = title,
            frequencyText = frequencySummary(),
            modeText = checkInMode.label(),
            statusText = todayStatus.label(),
            supportingText = supportingText(todayStatus),
            todayStatus = todayStatus,
            isDeleted = isDeleted,
            canQuickCheck = canQuickCheck,
            quickActionLabel = quickActionLabel,
            quickActionType = quickState.primaryActionType,
            quickActionEnabled = quickActionEnabled,
            canOpenDetail = !isDeleted,
            canRestore = isDeleted,
            updatedAt = updatedAt,
        )
    }

    private fun Habit.supportingText(todayStatus: HabitTodayStatus): String {
        if (isDeleted) return "该习惯已软删除，可直接恢复。"
        return when (checkInMode) {
            HabitCheckInMode.CHECK -> when (todayStatus) {
                HabitTodayStatus.DUE -> "今天可直接打卡。"
                HabitTodayStatus.COMPLETED -> "今天已完成。"
                HabitTodayStatus.NOT_DUE -> "今天无需执行。"
                HabitTodayStatus.DELETED -> "已删除。"
            }

            HabitCheckInMode.STEPS -> when (todayStatus) {
                HabitTodayStatus.DUE -> "${steps.size} 个步骤，进入详情完成。"
                HabitTodayStatus.COMPLETED -> "今天已完成全部步骤。"
                HabitTodayStatus.NOT_DUE -> "今天无需执行。"
                HabitTodayStatus.DELETED -> "已删除。"
            }

            HabitCheckInMode.DURATION -> {
                val target = targetDurationMinutes ?: 0
                when (todayStatus) {
                    HabitTodayStatus.DUE -> "目标时长 $target 分钟，进入详情开始计时。"
                    HabitTodayStatus.COMPLETED -> {
                        val actualDuration = todayRecord?.durationMinutes ?: target
                        "今天已记录 $actualDuration 分钟。"
                    }
                    HabitTodayStatus.NOT_DUE -> "今天无需执行。"
                    HabitTodayStatus.DELETED -> "已删除。"
                }
            }
        }
    }

    private fun sortBucket(item: HabitCardUiModel): Int {
        return when (item.todayStatus) {
            HabitTodayStatus.DUE -> 0
            HabitTodayStatus.COMPLETED -> 1
            HabitTodayStatus.NOT_DUE -> 2
            HabitTodayStatus.DELETED -> 3
        }
    }

    private fun todayEpochDay(): Long = currentDate().toEpochDay()

    private fun currentDate(): java.time.LocalDate {
        return Instant.ofEpochMilli(timeProvider.nowMillis())
            .atZone(timeProvider.zoneId())
            .toLocalDate()
    }
}
