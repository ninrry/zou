package luzzr.zou.core.reminder

import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.HabitFrequencyType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitReminderCalculatorTest {

    private val zoneId = ZoneId.of("Asia/Singapore")
    private val calculator = HabitReminderCalculator(
        object : TimeProvider {
            override fun nowMillis(): Long = 0L

            override fun zoneId(): ZoneId = zoneId
        },
    )

    @Test
    fun crossMidnightWeeklyWindowContinuesAfterMidnight() {
        val next = calculator.calculateNext(
            spec = spec(
                frequencyType = HabitFrequencyType.WEEKLY,
                selectedWeekdays = setOf(DayOfWeek.MONDAY),
                remindWindowStart = LocalTime.of(23, 0),
                remindWindowEnd = LocalTime.of(1, 0),
                repeatIntervalMinutes = 60,
            ),
            nowMillis = epochMillis(2026, 3, 10, 0, 30),
        )

        assertEquals(
            NextReminderOccurrence(
                atMillis = epochMillis(2026, 3, 10, 1, 0),
                reason = ReminderTriggerReason.REPEAT,
            ),
            next,
        )
    }

    @Test
    fun completedCrossMidnightOccurrenceSkipsCarryOverWindow() {
        val monday = LocalDate.of(2026, 3, 9)
        val next = calculator.calculateNext(
            spec = spec(
                frequencyType = HabitFrequencyType.WEEKLY,
                selectedWeekdays = setOf(DayOfWeek.MONDAY),
                remindWindowStart = LocalTime.of(23, 0),
                remindWindowEnd = LocalTime.of(1, 0),
                repeatIntervalMinutes = 60,
                completedEpochDays = setOf(monday.toEpochDay()),
            ),
            nowMillis = epochMillis(2026, 3, 10, 0, 30),
        )

        assertEquals(
            NextReminderOccurrence(
                atMillis = epochMillis(2026, 3, 16, 23, 0),
                reason = ReminderTriggerReason.START,
            ),
            next,
        )
    }

    private fun spec(
        frequencyType: HabitFrequencyType = HabitFrequencyType.DAILY,
        selectedWeekdays: Set<DayOfWeek> = emptySet(),
        remindWindowStart: LocalTime? = null,
        remindWindowEnd: LocalTime? = null,
        repeatIntervalMinutes: Int? = null,
        completedEpochDays: Set<Long> = emptySet(),
    ): HabitReminderSpec {
        return HabitReminderSpec(
            habitId = "habit-1",
            title = "Habit",
            frequencyType = frequencyType,
            selectedWeekdays = selectedWeekdays,
            intervalDays = null,
            intervalAnchorDate = null,
            monthlyDays = emptySet(),
            remindWindowStart = remindWindowStart,
            remindWindowEnd = remindWindowEnd,
            repeatIntervalMinutes = repeatIntervalMinutes,
            exactReminderTimes = emptyList(),
            isDeleted = false,
            archived = false,
            completedEpochDays = completedEpochDays,
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
