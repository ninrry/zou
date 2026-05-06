package luzzr.zou.domain.usecase

import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitTodayStatus
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskStatus
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

enum class TaskQuickActionType {
    COMPLETE,
    ADVANCE_SUBTASK,
    NONE,
}

data class TaskQuickActionState(
    val shouldShow: Boolean,
    val actionType: TaskQuickActionType,
    val actionLabel: String,
    val actionEnabled: Boolean,
)

enum class HabitQuickActionType {
    CHECK,
    STEP_ADVANCE,
    DURATION_TOGGLE,
    DURATION_FINISH,
    OPEN_DETAIL,
    NONE,
}

data class HabitQuickActionState(
    val shouldShow: Boolean,
    val primaryActionType: HabitQuickActionType,
    val primaryActionLabel: String,
    val primaryActionEnabled: Boolean,
    val secondaryActionType: HabitQuickActionType? = null,
    val secondaryActionLabel: String? = null,
    val secondaryActionEnabled: Boolean = false,
    val progressText: String? = null,
    val statusHint: String? = null,
    val durationRunning: Boolean = false,
)

@Singleton
class CheckInPolicy @Inject constructor(
    private val timeProvider: TimeProvider,
) {

    fun evaluateTaskQuickAction(
        task: Task,
        nowMillis: Long = timeProvider.nowMillis(),
    ): TaskQuickActionState {
        val overdue = task.dueAt != null && task.dueAt <= nowMillis
        if (task.status == TaskStatus.COMPLETED || overdue) {
            return TaskQuickActionState(
                shouldShow = false,
                actionType = TaskQuickActionType.NONE,
                actionLabel = "",
                actionEnabled = false,
            )
        }

        return if (task.completionRule == TaskCompletionRule.AUTO_ALL_SUBTASKS && task.subTasks.isNotEmpty()) {
            TaskQuickActionState(
                shouldShow = true,
                actionType = TaskQuickActionType.ADVANCE_SUBTASK,
                actionLabel = "下一步",
                actionEnabled = task.subTasks.any { !it.isCompleted },
            )
        } else {
            TaskQuickActionState(
                shouldShow = true,
                actionType = TaskQuickActionType.COMPLETE,
                actionLabel = "完成",
                actionEnabled = true,
            )
        }
    }

    fun evaluateHabitQuickAction(
        habit: Habit,
        todayStatus: HabitTodayStatus,
        nowMillis: Long = timeProvider.nowMillis(),
    ): HabitQuickActionState {
        if (habit.isDeleted || todayStatus == HabitTodayStatus.DELETED || todayStatus == HabitTodayStatus.COMPLETED) {
            return HabitQuickActionState(
                shouldShow = false,
                primaryActionType = HabitQuickActionType.NONE,
                primaryActionLabel = "",
                primaryActionEnabled = false,
            )
        }
        if (todayStatus != HabitTodayStatus.DUE) {
            return HabitQuickActionState(
                shouldShow = false,
                primaryActionType = HabitQuickActionType.NONE,
                primaryActionLabel = "",
                primaryActionEnabled = false,
            )
        }

        val inWindow = isWithinHabitWindow(habit, nowMillis)
        val statusHint = if (inWindow) null else "未到可打卡时间"

        return when (habit.checkInMode) {
            HabitCheckInMode.CHECK -> HabitQuickActionState(
                shouldShow = true,
                primaryActionType = HabitQuickActionType.CHECK,
                primaryActionLabel = "打卡",
                primaryActionEnabled = inWindow,
                statusHint = statusHint,
            )

            HabitCheckInMode.STEPS -> {
                val totalSteps = habit.steps.size
                val completed = habit.todayRecord?.stepProgressIds
                    ?.count { id -> habit.steps.any { it.id == id } }
                    ?: 0
                val nextIndex = (completed + 1).coerceAtMost(max(1, totalSteps))
                HabitQuickActionState(
                    shouldShow = true,
                    primaryActionType = HabitQuickActionType.STEP_ADVANCE,
                    primaryActionLabel = "下一步 $nextIndex/$totalSteps",
                    primaryActionEnabled = inWindow && totalSteps > 0 && completed < totalSteps,
                    progressText = "$completed/$totalSteps",
                    statusHint = when {
                        totalSteps <= 0 -> "请先配置步骤"
                        !inWindow -> "未到可打卡时间"
                        else -> null
                    },
                )
            }

            HabitCheckInMode.DURATION -> {
                val elapsedSeconds = currentDurationElapsedSeconds(habit, nowMillis)
                val targetMinutes = habit.targetDurationMinutes
                val canFinish = when {
                    !inWindow -> false
                    targetMinutes == null || targetMinutes <= 0 -> elapsedSeconds > 0L
                    else -> elapsedSeconds >= targetMinutes * 60L
                }
                val running = habit.todayRecord?.durationRunningSinceMillis != null
                HabitQuickActionState(
                    shouldShow = true,
                    primaryActionType = HabitQuickActionType.DURATION_TOGGLE,
                    primaryActionLabel = when {
                        running -> "暂停"
                        elapsedSeconds > 0L -> "继续"
                        else -> "开始"
                    },
                    primaryActionEnabled = inWindow,
                    secondaryActionType = HabitQuickActionType.DURATION_FINISH,
                    secondaryActionLabel = "完成",
                    secondaryActionEnabled = canFinish,
                    progressText = formatDurationProgress(elapsedSeconds, targetMinutes),
                    statusHint = statusHint,
                    durationRunning = running,
                )
            }
        }
    }

    fun isWithinHabitWindow(
        habit: Habit,
        nowMillis: Long = timeProvider.nowMillis(),
    ): Boolean {
        val start = habit.remindWindowStart
        val end = habit.remindWindowEnd
        if (start == null && end == null) return true

        val nowTime = Instant.ofEpochMilli(nowMillis)
            .atZone(timeProvider.zoneId())
            .toLocalTime()
        return when {
            start != null && end != null -> isWithinWindow(nowTime, start, end)
            start != null -> !nowTime.isBefore(start)
            else -> !nowTime.isAfter(requireNotNull(end))
        }
    }

    fun currentDurationElapsedSeconds(
        habit: Habit,
        nowMillis: Long = timeProvider.nowMillis(),
    ): Long {
        val record = habit.todayRecord ?: return 0L
        val base = record.durationElapsedSeconds.coerceAtLeast(0L)
        val runningSince = record.durationRunningSinceMillis ?: return base
        val delta = ((nowMillis - runningSince) / 1_000L).coerceAtLeast(0L)
        return base + delta
    }

    private fun isWithinWindow(
        current: LocalTime,
        start: LocalTime,
        end: LocalTime,
    ): Boolean {
        return if (end.isBefore(start)) {
            current >= start || current <= end
        } else {
            current >= start && current <= end
        }
    }

    private fun formatDurationProgress(
        elapsedSeconds: Long,
        targetMinutes: Int?,
    ): String {
        val elapsedMinutes = max(0, ((elapsedSeconds + 59L) / 60L).toInt())
        return if (targetMinutes == null || targetMinutes <= 0) {
            "已计时 $elapsedMinutes 分钟"
        } else {
            "$elapsedMinutes/$targetMinutes 分钟"
        }
    }
}
