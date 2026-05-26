package luzzr.zou.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.RemainingTimeFormatter
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.HabitTodayStatus
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.model.TodayOverview
import luzzr.zou.domain.usecase.AdvanceHabitStepUseCase
import luzzr.zou.domain.usecase.AdvanceTaskSubTaskUseCase
import luzzr.zou.domain.usecase.CompleteCheckHabitUseCase
import luzzr.zou.domain.usecase.FinishHabitDurationUseCase
import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.ObserveTodayOverviewUseCase
import luzzr.zou.domain.usecase.PauseHabitDurationUseCase
import luzzr.zou.domain.usecase.RevertHabitStepUseCase
import luzzr.zou.domain.usecase.StartHabitDurationUseCase
import luzzr.zou.domain.usecase.TaskQuickActionType
import luzzr.zou.domain.usecase.ToggleSubTaskCompletedUseCase
import luzzr.zou.domain.usecase.ToggleTaskCompletedUseCase
import luzzr.zou.domain.usecase.UndoHabitCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TodayViewModel @Inject constructor(
    observeTodayOverviewUseCase: ObserveTodayOverviewUseCase,
    private val toggleTaskCompletedUseCase: ToggleTaskCompletedUseCase,
    private val toggleSubTaskCompletedUseCase: ToggleSubTaskCompletedUseCase,
    private val advanceTaskSubTaskUseCase: AdvanceTaskSubTaskUseCase,
    private val completeCheckHabitUseCase: CompleteCheckHabitUseCase,
    private val advanceHabitStepUseCase: AdvanceHabitStepUseCase,
    private val revertHabitStepUseCase: RevertHabitStepUseCase,
    private val startHabitDurationUseCase: StartHabitDurationUseCase,
    private val pauseHabitDurationUseCase: PauseHabitDurationUseCase,
    private val finishHabitDurationUseCase: FinishHabitDurationUseCase,
    private val undoHabitCompletionUseCase: UndoHabitCompletionUseCase,
    private val timeProvider: TimeProvider,
    private val quickPreviewPolicy: TodayQuickPreviewPolicy,
    private val remainingTimeFormatter: RemainingTimeFormatter,
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
    private val undoActions = mutableMapOf<String, suspend () -> Unit>()
    private val _events = MutableSharedFlow<TodayUiEvent>(extraBufferCapacity = 16)
    val events: Flow<TodayUiEvent> = _events

    val uiState: StateFlow<TodayUiState> = combine(
        observeTodayOverviewUseCase(),
        minuteTickerFlow(),
    ) { overview, nowMillis ->
        mapUiState(overview, nowMillis)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(dateLine = dateFormatter.format(currentDate())),
    )

    fun onTaskAction(taskId: String, actionType: TaskQuickActionType) {
        viewModelScope.launch {
            when (actionType) {
                TaskQuickActionType.COMPLETE -> {
                    toggleTaskCompletedUseCase(taskId = taskId, completed = true)
                    emitUndo(
                        message = "任务已完成，10 秒内可撤销",
                        undoAction = { toggleTaskCompletedUseCase(taskId = taskId, completed = false) },
                    )
                }

                TaskQuickActionType.ADVANCE_SUBTASK -> {
                    val result = advanceTaskSubTaskUseCase(taskId)
                    val advancedId = result.progressedSubTaskId
                    if (advancedId == null) {
                        _events.emit(TodayUiEvent.ShowMessage("该任务没有可推进的子任务"))
                    } else {
                        emitUndo(
                            message = if (result.taskCompleted) {
                                "子任务已全部完成并自动完成任务，10 秒内可撤销"
                            } else {
                                "已推进子任务（${result.completedCount}/${result.totalCount}），10 秒内可撤销"
                            },
                            undoAction = {
                                toggleSubTaskCompletedUseCase(
                                    taskId = taskId,
                                    subTaskId = advancedId,
                                    completed = false,
                                )
                            },
                        )
                    }
                }

                TaskQuickActionType.NONE -> Unit
            }
        }
    }

    fun onHabitPrimaryAction(habitId: String, actionType: HabitQuickActionType, durationRunning: Boolean) {
        viewModelScope.launch {
            val recordDate = currentDate().toEpochDay()
            when (actionType) {
                HabitQuickActionType.CHECK -> {
                    completeCheckHabitUseCase(habitId = habitId, recordDate = recordDate)
                    emitUndo(
                        message = "习惯已打卡，10 秒内可撤销",
                        undoAction = { undoHabitCompletionUseCase(habitId = habitId, recordDate = recordDate) },
                    )
                }

                HabitQuickActionType.STEP_ADVANCE -> {
                    val result = advanceHabitStepUseCase(habitId = habitId, recordDate = recordDate)
                    val stepId = result.progressedStepId
                    if (stepId == null) {
                        _events.emit(TodayUiEvent.ShowMessage("已没有可推进的步骤"))
                    } else {
                        emitUndo(
                            message = if (result.habitCompleted) {
                                "步骤已全部完成并自动打卡，10 秒内可撤销"
                            } else {
                                "已推进步骤（${result.completedCount}/${result.totalCount}），10 秒内可撤销"
                            },
                            undoAction = {
                                revertHabitStepUseCase(
                                    habitId = habitId,
                                    recordDate = recordDate,
                                    stepId = stepId,
                                )
                            },
                        )
                    }
                }

                HabitQuickActionType.DURATION_TOGGLE -> {
                    if (durationRunning) {
                        pauseHabitDurationUseCase(habitId = habitId, recordDate = recordDate)
                        _events.emit(TodayUiEvent.ShowMessage("已暂停计时"))
                    } else {
                        startHabitDurationUseCase(habitId = habitId, recordDate = recordDate)
                        _events.emit(TodayUiEvent.ShowMessage("已开始计时"))
                    }
                }

                else -> Unit
            }
        }
    }

    fun onHabitSecondaryAction(habitId: String, actionType: HabitQuickActionType) {
        if (actionType != HabitQuickActionType.DURATION_FINISH) return
        viewModelScope.launch {
            val recordDate = currentDate().toEpochDay()
            val result = finishHabitDurationUseCase(
                habitId = habitId,
                recordDate = recordDate,
            )
            if (!result.completed) {
                val target = result.targetMinutes
                val elapsed = ((result.elapsedSeconds + 59L) / 60L).toInt()
                val text = if (target == null || target <= 0) {
                    "请先开始计时后再完成"
                } else {
                    "当前 $elapsed/$target 分钟，尚未达标"
                }
                _events.emit(TodayUiEvent.ShowMessage(text))
                return@launch
            }
            emitUndo(
                message = "时长习惯已完成，10 秒内可撤销",
                undoAction = { undoHabitCompletionUseCase(habitId = habitId, recordDate = recordDate) },
            )
        }
    }

    fun onUndo(tokenId: String) {
        val action = undoActions.remove(tokenId) ?: return
        viewModelScope.launch { action() }
    }

    fun onUndoExpired(tokenId: String) {
        undoActions.remove(tokenId)
    }

    private suspend fun emitUndo(
        message: String,
        undoAction: suspend () -> Unit,
    ) {
        val tokenId = UUID.randomUUID().toString()
        undoActions[tokenId] = undoAction
        _events.emit(TodayUiEvent.ShowUndo(message = message, tokenId = tokenId))
    }

    private fun mapUiState(
        overview: TodayOverview,
        nowMillis: Long,
    ): TodayUiState {
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(timeProvider.zoneId()).toLocalDate()

        val taskCards = overview.tasks.mapNotNull { item ->
            val quickState = quickPreviewPolicy.evaluateTask(
                task = item.task,
                nowMillis = nowMillis,
            )
            if (!quickState.shouldShow) {
                null
            } else {
                TodayTaskCardUiModel(
                    id = item.task.id,
                    title = item.task.title,
                    actionType = quickState.actionType,
                    actionLabel = quickState.actionLabel,
                    actionEnabled = quickState.actionEnabled,
                    remainingTimeText = remainingTimeFormatter.format(item.task.dueAt, nowMillis),
                    progressText = null,
                    isCompleted = item.task.status == luzzr.zou.domain.model.TaskStatus.COMPLETED,
                )
            }
        }

        val habitCards = overview.habits.mapNotNull { item ->
            val quickState = quickPreviewPolicy.evaluateHabit(
                habit = item.habit,
                todayStatus = item.todayStatus,
                nowMillis = nowMillis,
            )
            if (!quickState.shouldShow) {
                null
            } else {
                TodayHabitCardUiModel(
                    id = item.habit.id,
                    title = item.habit.title,
                    primaryActionType = quickState.primaryActionType,
                    primaryActionLabel = quickState.primaryActionLabel,
                    primaryActionEnabled = quickState.primaryActionEnabled,
                    secondaryActionType = quickState.secondaryActionType,
                    secondaryActionLabel = quickState.secondaryActionLabel,
                    secondaryActionEnabled = quickState.secondaryActionEnabled,
                    progressText = quickState.progressText,
                    statusHint = quickState.statusHint,
                    durationRunning = quickState.durationRunning,
                    isCompleted = item.todayStatus == luzzr.zou.domain.model.HabitTodayStatus.COMPLETED,
                )
            }
        }

        val completedCount = overview.tasks.count { item ->
            item.task.status == luzzr.zou.domain.model.TaskStatus.COMPLETED && Instant.ofEpochMilli(item.task.updatedAt)
                .atZone(timeProvider.zoneId())
                .toLocalDate() == nowDate
        } + overview.habits.count { it.todayStatus == luzzr.zou.domain.model.HabitTodayStatus.COMPLETED }

        return TodayUiState(
            title = "今日",
            dateLine = dateFormatter.format(nowDate),
            summary = TodaySummaryUiModel(
                pendingTaskCount = taskCards.size,
                dueHabitCount = habitCards.size,
                completedCount = completedCount,
            ),
            tasks = taskCards,
            habits = habitCards,
            recentNotes = emptyList(),
            isCompletelyEmpty = taskCards.isEmpty() && habitCards.isEmpty(),
        )
    }

    private fun minuteTickerFlow(): Flow<Long> = flow {
        emit(timeProvider.nowMillis())
        while (true) {
            delay(60_000L)
            emit(timeProvider.nowMillis())
        }
    }

    private fun currentDate(): LocalDate {
        return Instant.ofEpochMilli(timeProvider.nowMillis())
            .atZone(timeProvider.zoneId())
            .toLocalDate()
    }
}
