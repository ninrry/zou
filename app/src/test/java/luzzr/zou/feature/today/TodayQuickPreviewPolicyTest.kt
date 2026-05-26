package luzzr.zou.feature.today

import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitTodayStatus
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.usecase.CheckInPolicy
import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.TaskQuickActionType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayQuickPreviewPolicyTest {

    private val timeProvider = FixedTimeProvider(
        date = LocalDate.of(2026, 3, 13),
        time = LocalTime.of(9, 0),
    )
    private val policy = TodayQuickPreviewPolicy(CheckInPolicy(timeProvider))

    @Test
    fun `due check habit should be visible and actionable`() {
        val result = policy.evaluateHabit(
            habit = Habit(
                id = "habit-check",
                title = "喝水",
                checkInMode = HabitCheckInMode.CHECK,
            ),
            todayStatus = HabitTodayStatus.DUE,
            nowMillis = timeProvider.nowMillis(),
        )

        assertTrue(result.shouldShow)
        assertEquals(HabitQuickActionType.CHECK, result.primaryActionType)
        assertTrue(result.primaryActionEnabled)
    }

    @Test
    fun `due habit out of window should stay visible but disabled`() {
        val result = policy.evaluateHabit(
            habit = Habit(
                id = "habit-window",
                title = "早起跑步",
                checkInMode = HabitCheckInMode.CHECK,
                remindWindowStart = LocalTime.of(10, 0),
                remindWindowEnd = LocalTime.of(12, 0),
            ),
            todayStatus = HabitTodayStatus.DUE,
            nowMillis = timeProvider.nowMillis(),
        )

        assertTrue(result.shouldShow)
        assertFalse(result.primaryActionEnabled)
    }

    @Test
    fun `auto task with subtask should expose advance action`() {
        val result = policy.evaluateTask(
            task = Task(
                id = "task-auto",
                title = "整理项目",
                status = TaskStatus.ACTIVE,
                completionRule = TaskCompletionRule.AUTO_ALL_SUBTASKS,
                dueAt = timeProvider.nowMillis() + 60_000L,
                subTasks = listOf(SubTask(id = "sub-1", title = "第一步")),
            ),
            nowMillis = timeProvider.nowMillis(),
        )

        assertTrue(result.shouldShow)
        assertEquals(TaskQuickActionType.ADVANCE_SUBTASK, result.actionType)
        assertTrue(result.actionEnabled)
    }

    @Test
    fun `overdue task should be hidden from quick preview`() {
        val result = policy.evaluateTask(
            task = Task(
                id = "task-overdue",
                title = "逾期任务",
                status = TaskStatus.ACTIVE,
                completionRule = TaskCompletionRule.MANUAL,
                dueAt = timeProvider.nowMillis() - 1_000L,
            ),
            nowMillis = timeProvider.nowMillis(),
        )

        assertFalse(result.shouldShow)
        assertEquals(TaskQuickActionType.NONE, result.actionType)
    }

    private class FixedTimeProvider(
        private val date: LocalDate,
        private val time: LocalTime,
    ) : TimeProvider {
        override fun nowMillis(): Long = date
            .atTime(time)
            .atZone(zoneId())
            .toInstant()
            .toEpochMilli()

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }
}
