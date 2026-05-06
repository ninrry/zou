package luzzr.zou.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitTodayStatus
import luzzr.zou.domain.usecase.AdvanceHabitStepUseCase
import luzzr.zou.domain.usecase.CheckInPolicy
import luzzr.zou.domain.usecase.CompleteCheckHabitUseCase
import luzzr.zou.domain.usecase.CompleteStepsHabitUseCase
import luzzr.zou.domain.usecase.FinishHabitDurationUseCase
import luzzr.zou.domain.usecase.GetHabitUseCase
import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.HabitScheduleEvaluator
import luzzr.zou.domain.usecase.PauseHabitDurationUseCase
import luzzr.zou.domain.usecase.RevertHabitStepUseCase
import luzzr.zou.domain.usecase.SoftDeleteHabitUseCase
import luzzr.zou.domain.usecase.StartHabitDurationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHabitUseCase: GetHabitUseCase,
    private val completeCheckHabitUseCase: CompleteCheckHabitUseCase,
    private val completeStepsHabitUseCase: CompleteStepsHabitUseCase,
    private val advanceHabitStepUseCase: AdvanceHabitStepUseCase,
    private val revertHabitStepUseCase: RevertHabitStepUseCase,
    private val startHabitDurationUseCase: StartHabitDurationUseCase,
    private val pauseHabitDurationUseCase: PauseHabitDurationUseCase,
    private val finishHabitDurationUseCase: FinishHabitDurationUseCase,
    private val softDeleteHabitUseCase: SoftDeleteHabitUseCase,
    private val habitScheduleEvaluator: HabitScheduleEvaluator,
    private val checkInPolicy: CheckInPolicy,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val habitId: String? = savedStateHandle[HabitRoutes.habitIdArg]
    private var currentHabit: Habit? = null
    private var todayStatus: HabitTodayStatus = HabitTodayStatus.NOT_DUE

    private var durationTickerJob: Job? = null

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    init {
        loadHabit()
    }

    override fun onCleared() {
        stopDurationTicker()
        super.onCleared()
    }

    fun onStepCheckedChanged(stepId: String, checked: Boolean) {
        val habit = currentHabit ?: return
        if (habit.checkInMode != HabitCheckInMode.STEPS) return
        if (!canOperateNow(habit)) {
            _uiState.update { it.copy(actionMessage = "今天不在执行窗口内，暂不可打卡。") }
            return
        }
        viewModelScope.launch {
            if (checked) {
                val result = advanceHabitStepUseCase(
                    habitId = habit.id,
                    recordDate = currentDate().toEpochDay(),
                )
                loadHabit(
                    clearActionMessage = false,
                    postLoadMessage = when {
                        result.progressedStepId == null -> "没有可推进的步骤。"
                        result.habitCompleted -> "全部步骤完成，已自动打卡。"
                        else -> "已完成 ${result.completedCount}/${result.totalCount} 步。"
                    },
                )
            } else {
                val result = revertHabitStepUseCase(
                    habitId = habit.id,
                    recordDate = currentDate().toEpochDay(),
                    stepId = stepId,
                )
                loadHabit(
                    clearActionMessage = false,
                    postLoadMessage = if (result.progressedStepId == null) {
                        "该步骤当前未完成，无需撤销。"
                    } else {
                        "已撤销一步，当前 ${result.completedCount}/${result.totalCount}。"
                    },
                )
            }
        }
    }

    fun onDurationTimerToggle() {
        val habit = currentHabit ?: return
        val state = uiState.value
        if (habit.checkInMode != HabitCheckInMode.DURATION) return
        if (!state.canCheckInToday) {
            _uiState.update { it.copy(actionMessage = "今天不在执行窗口内，暂不可打卡。") }
            return
        }

        viewModelScope.launch {
            if (state.durationTimerRunning) {
                pauseHabitDurationUseCase(
                    habitId = habit.id,
                    recordDate = currentDate().toEpochDay(),
                )
                loadHabit(clearActionMessage = false, postLoadMessage = "已暂停计时。")
            } else {
                startHabitDurationUseCase(
                    habitId = habit.id,
                    recordDate = currentDate().toEpochDay(),
                )
                loadHabit(
                    clearActionMessage = false,
                    postLoadMessage = if (state.durationElapsedSeconds > 0L) "已继续计时。" else "已开始计时。",
                )
            }
        }
    }

    fun onDurationFinish() {
        val habit = currentHabit ?: return
        if (habit.checkInMode != HabitCheckInMode.DURATION) return
        if (!canOperateNow(habit)) {
            _uiState.update { it.copy(actionMessage = "今天不在执行窗口内，暂不可打卡。") }
            return
        }
        viewModelScope.launch {
            val result = finishHabitDurationUseCase(
                habitId = habit.id,
                recordDate = currentDate().toEpochDay(),
            )
            if (!result.completed) {
                val elapsedMinutes = ((result.elapsedSeconds + 59L) / 60L).toInt()
                val target = result.targetMinutes
                _uiState.update {
                    it.copy(
                        actionMessage = if (target == null || target <= 0) {
                            "请先开始计时后再完成。"
                        } else {
                            "当前 $elapsedMinutes/$target 分钟，未达标。"
                        },
                    )
                }
                return@launch
            }
            loadHabit(clearActionMessage = false, postLoadMessage = "时长已达标，今日打卡完成。")
        }
    }

    fun onCompleteClicked() {
        val habit = currentHabit ?: return
        if (!canOperateNow(habit)) {
            _uiState.update { it.copy(actionMessage = "今天不在执行窗口内，暂不可打卡。") }
            return
        }

        when (habit.checkInMode) {
            HabitCheckInMode.CHECK -> completeCheckHabit(habit.id)
            HabitCheckInMode.STEPS -> completeStepsHabit(habit)
            HabitCheckInMode.DURATION -> onDurationFinish()
        }
    }

    fun onDeleteClicked(onDeleted: () -> Unit = {}) {
        val currentHabitId = habitId ?: return
        viewModelScope.launch {
            softDeleteHabitUseCase(currentHabitId)
            onDeleted()
        }
    }

    private fun stopDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = null
    }

    private fun startDurationTickerIfNeeded(habit: Habit) {
        if (habit.checkInMode != HabitCheckInMode.DURATION) {
            stopDurationTicker()
            return
        }
        if (habit.todayRecord?.durationRunningSinceMillis == null) {
            stopDurationTicker()
            return
        }
        durationTickerJob?.cancel()
        durationTickerJob = viewModelScope.launch {
            while (true) {
                refreshDurationUi(habit, clearMessage = false)
                delay(1_000L)
            }
        }
    }

    private fun formatElapsed(seconds: Long): String {
        val hours = seconds / 3_600
        val minutes = (seconds % 3_600) / 60
        val remainSeconds = seconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, remainSeconds)
    }

    private fun refreshDurationUi(
        habit: Habit,
        clearMessage: Boolean,
    ) {
        val elapsedSeconds = checkInPolicy.currentDurationElapsedSeconds(habit)
        val quickState = checkInPolicy.evaluateHabitQuickAction(
            habit = habit,
            todayStatus = todayStatus,
            nowMillis = timeProvider.nowMillis(),
        )
        _uiState.update { state ->
            state.copy(
                durationElapsedSeconds = elapsedSeconds,
                durationElapsedText = formatElapsed(elapsedSeconds),
                durationInput = ((elapsedSeconds + 59L) / 60L).toInt()
                    .takeIf { it > 0 }
                    ?.toString()
                    .orEmpty(),
                durationTimerRunning = habit.todayRecord?.durationRunningSinceMillis != null,
                durationTimerActionLabel = quickState.primaryActionLabel.ifBlank {
                    if (habit.todayRecord?.durationRunningSinceMillis != null) "暂停" else "开始"
                },
                durationCanFinish = quickState.secondaryActionType == HabitQuickActionType.DURATION_FINISH &&
                    quickState.secondaryActionEnabled,
                actionMessage = if (clearMessage) null else state.actionMessage,
            )
        }
    }

    private fun completeCheckHabit(currentHabitId: String) {
        viewModelScope.launch {
            completeCheckHabitUseCase(
                habitId = currentHabitId,
                recordDate = currentDate().toEpochDay(),
            )
            loadHabit(clearActionMessage = false, postLoadMessage = "打卡成功。")
        }
    }

    private fun completeStepsHabit(habit: Habit) {
        val state = uiState.value
        if (state.steps.any { !it.checked }) {
            _uiState.update { it.copy(actionMessage = "请先完成全部步骤。") }
            return
        }
        viewModelScope.launch {
            completeStepsHabitUseCase(
                habitId = habit.id,
                recordDate = currentDate().toEpochDay(),
            )
            loadHabit(clearActionMessage = false, postLoadMessage = "全部步骤已完成并打卡。")
        }
    }

    private fun loadHabit(
        clearActionMessage: Boolean = true,
        postLoadMessage: String? = null,
    ) {
        val currentHabitId = habitId
        if (currentHabitId.isNullOrBlank()) {
            _uiState.value = HabitDetailUiState(
                isLoading = false,
                emptyMessage = "习惯不存在或已删除。",
            )
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    emptyMessage = null,
                    actionMessage = if (clearActionMessage) null else it.actionMessage,
                )
            }
            val habit = getHabitUseCase(
                habitId = currentHabitId,
                recordDate = currentDate().toEpochDay(),
            )
            if (habit == null || habit.isDeleted) {
                stopDurationTicker()
                currentHabit = null
                _uiState.value = HabitDetailUiState(
                    isLoading = false,
                    emptyMessage = "习惯不存在或已删除。",
                )
                return@launch
            }

            currentHabit = habit
            todayStatus = habitScheduleEvaluator.getTodayStatus(
                habit = habit,
                date = currentDate(),
                todayRecord = habit.todayRecord,
            )
            _uiState.value = habit.toUiState()
            refreshDurationUi(habit = habit, clearMessage = true)
            startDurationTickerIfNeeded(habit)
            if (!postLoadMessage.isNullOrBlank()) {
                _uiState.update { it.copy(actionMessage = postLoadMessage) }
            }
        }
    }

    private fun Habit.toUiState(): HabitDetailUiState {
        val canCheckIn = canOperateNow(this)
        val completedStepIds = todayRecord?.stepProgressIds.orEmpty()
        val stepsState = steps
            .sortedBy { it.sortOrder }
            .map { step ->
                HabitStepProgressUiModel(
                    id = step.id,
                    title = step.title,
                    checked = completedStepIds.contains(step.id),
                )
            }

        return HabitDetailUiState(
            habitId = id,
            title = title,
            frequencyText = frequencySummary(),
            todayStatusText = todayStatus.label(),
            modeText = checkInMode.label(),
            helperText = helperTextFor(todayStatus),
            contentMarkdown = contentMarkdown.orEmpty(),
            steps = stepsState,
            durationInput = "",
            durationTargetText = durationTargetText(),
            showCheckSection = checkInMode == HabitCheckInMode.CHECK,
            showStepsSection = checkInMode == HabitCheckInMode.STEPS,
            showDurationSection = checkInMode == HabitCheckInMode.DURATION,
            canCheckInToday = canCheckIn,
            completeButtonLabel = completeButtonLabelFor(todayStatus),
            completeEnabled = canEnableCompletion(
                habit = this,
                canCheckIn = canCheckIn,
                steps = stepsState,
            ),
            canEdit = true,
            canDelete = true,
            isLoading = false,
            emptyMessage = null,
        )
    }

    private fun canOperateNow(habit: Habit): Boolean {
        return !habit.isDeleted &&
            todayStatus == HabitTodayStatus.DUE &&
            checkInPolicy.isWithinHabitWindow(habit = habit, nowMillis = timeProvider.nowMillis())
    }

    private fun Habit.helperTextFor(status: HabitTodayStatus): String {
        return when (status) {
            HabitTodayStatus.COMPLETED -> "今天已完成，明天会重新开始。"
            HabitTodayStatus.DUE -> when (checkInMode) {
                HabitCheckInMode.CHECK -> "点击一次即可完成今日打卡。"
                HabitCheckInMode.STEPS -> "完成全部步骤后，再点击完成。"
                HabitCheckInMode.DURATION -> "先开始计时，达到目标后会自动完成。"
            }
            HabitTodayStatus.NOT_DUE -> "今天不在执行周期内，暂不可打卡。"
            HabitTodayStatus.DELETED -> "已删除习惯不可打卡。"
        }
    }

    private fun Habit.durationTargetText(): String {
        val target = targetDurationMinutes
        return if (target == null || target <= 0) {
            "开始计时后可随时结束并打卡。"
        } else {
            "目标时长 $target 分钟，达到后自动完成。"
        }
    }

    private fun Habit.completeButtonLabelFor(status: HabitTodayStatus): String {
        if (status == HabitTodayStatus.COMPLETED) return "今天已完成"
        return when (checkInMode) {
            HabitCheckInMode.CHECK -> "完成今日习惯"
            HabitCheckInMode.STEPS -> "完成全部步骤"
            HabitCheckInMode.DURATION -> "完成"
        }
    }

    private fun canEnableCompletion(
        habit: Habit,
        canCheckIn: Boolean,
        steps: List<HabitStepProgressUiModel>,
    ): Boolean {
        if (!canCheckIn) return false
        return when (habit.checkInMode) {
            HabitCheckInMode.CHECK -> true
            HabitCheckInMode.STEPS -> steps.isNotEmpty() && steps.all { it.checked }
            HabitCheckInMode.DURATION -> false
        }
    }

    private fun currentDate(): LocalDate {
        return Instant.ofEpochMilli(timeProvider.nowMillis())
            .atZone(timeProvider.zoneId())
            .toLocalDate()
    }
}
