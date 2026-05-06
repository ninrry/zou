package luzzr.zou.core.reminder

import luzzr.zou.core.time.TimeProvider
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskReminderCalculatorTest {

    private val zoneId = ZoneId.of("Asia/Singapore")
    private val timeProvider = object : TimeProvider {
        override fun nowMillis(): Long = 0L
        override fun zoneId(): ZoneId = zoneId
    }
    private val calculator = TaskReminderCalculator(timeProvider)

    @Test
    fun startOnlyReminderReturnsStartOccurrence() {
        val now = epochMillis(2026, 3, 10, 8, 0)
        val next = calculator.calculateNext(
            spec = spec(startReminderMinuteOfDay = 9 * 60),
            nowMillis = now,
        )

        assertEquals(
            NextReminderOccurrence(epochMillis(2026, 3, 10, 9, 0), ReminderTriggerReason.START),
            next,
        )
    }

    @Test
    fun repeatReminderStopsAfterWindowEndWhenDueIsSameDay() {
        val dueAt = epochMillis(2026, 3, 10, 10, 0)
        val next = calculator.calculateNext(
            spec = spec(
                startReminderMinuteOfDay = 9 * 60,
                windowEndMinuteOfDay = 9 * 60 + 30,
                repeatIntervalMinutes = 10,
                dueAt = dueAt,
            ),
            nowMillis = epochMillis(2026, 3, 10, 9, 31),
        )

        assertNull(next)
    }

    @Test
    fun repeatReminderNeedsWindowEnd() {
        val next = calculator.calculateNext(
            spec = spec(
                startReminderMinuteOfDay = 9 * 60,
                repeatIntervalMinutes = 10,
                dueAt = epochMillis(2026, 3, 10, 23, 59),
            ),
            nowMillis = epochMillis(2026, 3, 10, 9, 1),
        )

        assertNull(next)
    }

    @Test
    fun exactReminderCanTriggerOutsideWindow() {
        val next = calculator.calculateNext(
            spec = spec(
                startReminderMinuteOfDay = 9 * 60,
                windowEndMinuteOfDay = 10 * 60,
                repeatIntervalMinutes = 10,
                exactReminderTimes = listOf(epochMillis(2026, 3, 10, 22, 0)),
            ),
            nowMillis = epochMillis(2026, 3, 10, 21, 0),
        )

        assertEquals(
            NextReminderOccurrence(epochMillis(2026, 3, 10, 22, 0), ReminderTriggerReason.EXACT),
            next,
        )
    }

    @Test
    fun exactReminderWinsWhenEarlierThanWindowRepeat() {
        val next = calculator.calculateNext(
            spec = spec(
                startReminderMinuteOfDay = 9 * 60,
                windowEndMinuteOfDay = 18 * 60,
                repeatIntervalMinutes = 30,
                exactReminderTimes = listOf(epochMillis(2026, 3, 10, 9, 5)),
            ),
            nowMillis = epochMillis(2026, 3, 10, 9, 1),
        )

        assertEquals(
            NextReminderOccurrence(epochMillis(2026, 3, 10, 9, 5), ReminderTriggerReason.EXACT),
            next,
        )
    }

    @Test
    fun completedTaskHasNoNextReminder() {
        val next = calculator.calculateNext(
            spec = spec(
                startReminderMinuteOfDay = 9 * 60,
                isCompleted = true,
            ),
            nowMillis = epochMillis(2026, 3, 10, 8, 0),
        )

        assertNull(next)
    }

    private fun spec(
        taskId: String = "task-1",
        startReminderMinuteOfDay: Int? = null,
        windowEndMinuteOfDay: Int? = null,
        dueAt: Long? = null,
        repeatIntervalMinutes: Int? = null,
        exactReminderTimes: List<Long> = emptyList(),
        isCompleted: Boolean = false,
        isDeleted: Boolean = false,
    ): TaskReminderSpec {
        return TaskReminderSpec(
            taskId = taskId,
            title = "提醒任务",
            isCompleted = isCompleted,
            isDeleted = isDeleted,
            archived = false,
            startReminderMinuteOfDay = startReminderMinuteOfDay,
            windowEndMinuteOfDay = windowEndMinuteOfDay,
            dueAt = dueAt,
            repeatIntervalMinutes = repeatIntervalMinutes,
            exactReminderTimes = exactReminderTimes,
            allDay = false,
        )
    }

    private fun epochMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
