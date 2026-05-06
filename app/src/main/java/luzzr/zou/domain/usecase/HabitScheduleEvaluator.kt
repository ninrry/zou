package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitRecord
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.model.HabitTodayStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitScheduleEvaluator @Inject constructor() {

    fun isDueOn(
        habit: Habit,
        date: LocalDate,
    ): Boolean {
        if (habit.isDeleted || habit.archived) return false

        return when (habit.frequencyType) {
            HabitFrequencyType.DAILY -> true
            HabitFrequencyType.WEEKLY -> habit.selectedWeekdays.contains(date.dayOfWeek)
            HabitFrequencyType.INTERVAL_DAYS -> {
                val intervalDays = habit.intervalDays ?: return false
                val anchorDate = habit.intervalAnchorDate ?: return false
                if (intervalDays <= 0 || date < anchorDate) {
                    false
                } else {
                    ChronoUnit.DAYS.between(anchorDate, date) % intervalDays.toLong() == 0L
                }
            }
            HabitFrequencyType.MONTHLY -> habit.monthlyDays.contains(date.dayOfMonth)
        }
    }

    fun getTodayStatus(
        habit: Habit,
        date: LocalDate,
        todayRecord: HabitRecord?,
    ): HabitTodayStatus {
        if (habit.isDeleted) return HabitTodayStatus.DELETED
        return if (todayRecord?.status == HabitRecordStatus.COMPLETED) {
            HabitTodayStatus.COMPLETED
        } else if (isDueOn(habit, date)) {
            HabitTodayStatus.DUE
        } else {
            HabitTodayStatus.NOT_DUE
        }
    }
}
