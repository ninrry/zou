package luzzr.zou.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class Habit(
    val id: String,
    val title: String,
    val contentMarkdown: String? = null,
    val frequencyType: HabitFrequencyType = HabitFrequencyType.DAILY,
    val selectedWeekdays: Set<DayOfWeek> = emptySet(),
    val intervalDays: Int? = null,
    val intervalAnchorDate: LocalDate? = null,
    val monthlyDays: Set<Int> = emptySet(),
    val remindWindowStart: LocalTime? = null,
    val remindWindowEnd: LocalTime? = null,
    val repeatIntervalMinutes: Int? = null,
    val exactReminderTimes: List<LocalTime> = emptyList(),
    val checkInMode: HabitCheckInMode = HabitCheckInMode.CHECK,
    val targetDurationMinutes: Int? = null,
    val streakCountCache: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val tags: String? = null,
    val archived: Boolean = false,
    val steps: List<HabitStep> = emptyList(),
    val todayRecord: HabitRecord? = null,
    val reminderNotificationTitle: String? = null,
    val reminderNotificationBody: String? = null,
) {
    val hasReminderConfiguration: Boolean
        get() = remindWindowStart != null || exactReminderTimes.isNotEmpty()
}
