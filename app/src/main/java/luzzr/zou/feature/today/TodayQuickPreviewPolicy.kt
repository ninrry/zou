package luzzr.zou.feature.today

import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitTodayStatus
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.usecase.CheckInPolicy
import luzzr.zou.domain.usecase.HabitQuickActionState
import luzzr.zou.domain.usecase.TaskQuickActionState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodayQuickPreviewPolicy @Inject constructor(
    private val checkInPolicy: CheckInPolicy,
) {

    fun evaluateTask(
        task: Task,
        nowMillis: Long,
    ): TaskQuickActionState {
        return checkInPolicy.evaluateTaskQuickAction(
            task = task,
            nowMillis = nowMillis,
        )
    }

    fun evaluateHabit(
        habit: Habit,
        todayStatus: HabitTodayStatus,
        nowMillis: Long,
    ): HabitQuickActionState {
        return checkInPolicy.evaluateHabitQuickAction(
            habit = habit,
            todayStatus = todayStatus,
            nowMillis = nowMillis,
        )
    }
}
