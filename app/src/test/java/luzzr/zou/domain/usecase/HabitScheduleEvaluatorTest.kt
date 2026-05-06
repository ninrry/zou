package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitRecord
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.model.HabitTodayStatus
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitScheduleEvaluatorTest {

    private val evaluator = HabitScheduleEvaluator()

    @Test
    fun dailyHabitIsDueEveryDay() {
        val habit = habit(frequencyType = HabitFrequencyType.DAILY)

        assertTrue(evaluator.isDueOn(habit, LocalDate.of(2026, 3, 9)))
        assertTrue(evaluator.isDueOn(habit, LocalDate.of(2026, 3, 10)))
    }

    @Test
    fun weeklyHabitOnlyMatchesSelectedWeekdays() {
        val habit = habit(
            frequencyType = HabitFrequencyType.WEEKLY,
            selectedWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )

        assertTrue(evaluator.isDueOn(habit, LocalDate.of(2026, 3, 9)))
        assertFalse(evaluator.isDueOn(habit, LocalDate.of(2026, 3, 10)))
    }

    @Test
    fun intervalHabitUsesAnchorDateModulo() {
        val habit = habit(
            frequencyType = HabitFrequencyType.INTERVAL_DAYS,
            intervalDays = 3,
            intervalAnchorDate = LocalDate.of(2026, 3, 9),
        )

        assertTrue(evaluator.isDueOn(habit, LocalDate.of(2026, 3, 9)))
        assertFalse(evaluator.isDueOn(habit, LocalDate.of(2026, 3, 10)))
        assertTrue(evaluator.isDueOn(habit, LocalDate.of(2026, 3, 12)))
    }

    @Test
    fun monthlyHabitSkipsDaysThatDoNotExistInShortMonth() {
        val habit = habit(
            frequencyType = HabitFrequencyType.MONTHLY,
            monthlyDays = setOf(31),
        )

        assertFalse(evaluator.isDueOn(habit, LocalDate.of(2026, 4, 30)))
        assertTrue(evaluator.isDueOn(habit, LocalDate.of(2026, 5, 31)))
    }

    @Test
    fun completedRecordWinsOverDueState() {
        val date = LocalDate.of(2026, 3, 9)
        val habit = habit(frequencyType = HabitFrequencyType.DAILY)
        val record = HabitRecord(
            id = "record-1",
            habitId = habit.id,
            recordDate = date.toEpochDay(),
            status = HabitRecordStatus.COMPLETED,
        )

        assertEquals(
            HabitTodayStatus.COMPLETED,
            evaluator.getTodayStatus(habit, date, record),
        )
    }

    @Test
    fun deletedHabitIsNeverDue() {
        val habit = habit(
            frequencyType = HabitFrequencyType.DAILY,
            isDeleted = true,
        )

        assertFalse(evaluator.isDueOn(habit, LocalDate.of(2026, 3, 9)))
        assertEquals(
            HabitTodayStatus.DELETED,
            evaluator.getTodayStatus(habit, LocalDate.of(2026, 3, 9), null),
        )
    }

    private fun habit(
        id: String = "habit-1",
        frequencyType: HabitFrequencyType,
        selectedWeekdays: Set<DayOfWeek> = emptySet(),
        intervalDays: Int? = null,
        intervalAnchorDate: LocalDate? = null,
        monthlyDays: Set<Int> = emptySet(),
        isDeleted: Boolean = false,
    ): Habit {
        return Habit(
            id = id,
            title = "Habit",
            frequencyType = frequencyType,
            selectedWeekdays = selectedWeekdays,
            intervalDays = intervalDays,
            intervalAnchorDate = intervalAnchorDate,
            monthlyDays = monthlyDays,
            checkInMode = HabitCheckInMode.CHECK,
            isDeleted = isDeleted,
        )
    }
}
