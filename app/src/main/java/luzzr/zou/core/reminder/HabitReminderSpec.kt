package luzzr.zou.core.reminder

import luzzr.zou.domain.model.HabitFrequencyType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class HabitReminderSpec(
    val habitId: String,
    val title: String,
    val frequencyType: HabitFrequencyType,
    val selectedWeekdays: Set<DayOfWeek>,
    val intervalDays: Int?,
    val intervalAnchorDate: LocalDate?,
    val monthlyDays: Set<Int>,
    val remindWindowStart: LocalTime?,
    val remindWindowEnd: LocalTime?,
    val repeatIntervalMinutes: Int?,
    val exactReminderTimes: List<LocalTime>,
    val isDeleted: Boolean,
    val archived: Boolean,
    val completedEpochDays: Set<Long> = emptySet(),
)
