package luzzr.zou.feature.habits

import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitTodayStatus
import java.time.DayOfWeek

internal fun Habit.frequencySummary(): String {
    return when (frequencyType) {
        HabitFrequencyType.DAILY -> "每日"
        HabitFrequencyType.WEEKLY -> {
            val weekdayText = selectedWeekdays
                .sortedBy { it.value }
                .joinToString("、") { it.shortChineseLabel() }
            "每周 $weekdayText"
        }

        HabitFrequencyType.INTERVAL_DAYS -> {
            val interval = intervalDays ?: 1
            val anchor = intervalAnchorDate?.toString().orEmpty()
            "每隔 $interval 天（起始于 $anchor）"
        }

        HabitFrequencyType.MONTHLY -> {
            val monthlyText = monthlyDays.sorted().joinToString("、")
            "每月 $monthlyText 日"
        }
    }
}

internal fun HabitCheckInMode.label(): String {
    return when (this) {
        HabitCheckInMode.CHECK -> "勾选型"
        HabitCheckInMode.STEPS -> "步骤型"
        HabitCheckInMode.DURATION -> "时长型"
    }
}

internal fun HabitTodayStatus.label(): String {
    return when (this) {
        HabitTodayStatus.DUE -> "今日应执行"
        HabitTodayStatus.COMPLETED -> "今日已完成"
        HabitTodayStatus.NOT_DUE -> "今日无需执行"
        HabitTodayStatus.DELETED -> "已删除"
    }
}

internal fun DayOfWeek.shortChineseLabel(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}
