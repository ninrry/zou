package luzzr.zou.core.reminder

import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.HabitFrequencyType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitReminderCalculator @Inject constructor(
    private val timeProvider: TimeProvider,
) {

    fun calculateNext(
        spec: HabitReminderSpec,
        nowMillis: Long,
    ): NextReminderOccurrence? {
        if (spec.isDeleted || spec.archived) return null
        if (spec.remindWindowStart == null && spec.exactReminderTimes.isEmpty()) return null

        val zoneId = timeProvider.zoneId()
        val startDate = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .let { currentDate ->
                if (reminderWindowCrossesMidnight(spec.remindWindowStart, spec.remindWindowEnd)) {
                    currentDate.minusDays(1)
                } else {
                    currentDate
                }
            }

        repeat(367) { offset ->
            val date = startDate.plusDays(offset.toLong())
            if (!isDueOn(spec, date)) return@repeat
            if (date.toEpochDay() in spec.completedEpochDays) return@repeat

            val occurrence = buildCandidates(spec, date)
                .filter { candidate -> candidate.atMillis >= nowMillis }
                .minWithOrNull(compareBy<NextReminderOccurrence>({ it.atMillis }, { it.reason.priority() }))
            if (occurrence != null) {
                return occurrence
            }
        }
        return null
    }

    private fun buildCandidates(
        spec: HabitReminderSpec,
        date: LocalDate,
    ): List<NextReminderOccurrence> {
        val zoneId = timeProvider.zoneId()
        val candidates = mutableListOf<NextReminderOccurrence>()
        spec.exactReminderTimes.forEach { reminderTime ->
            candidates += NextReminderOccurrence(
                atMillis = LocalDateTime.of(date, reminderTime).atZone(zoneId).toInstant().toEpochMilli(),
                reason = ReminderTriggerReason.EXACT,
            )
        }

        val windowStart = spec.remindWindowStart
        val windowEnd = spec.remindWindowEnd
        val interval = spec.repeatIntervalMinutes
        if (windowStart != null) {
            if (interval == null || windowEnd == null) {
                candidates += NextReminderOccurrence(
                        atMillis = LocalDateTime.of(date, windowStart).atZone(zoneId).toInstant().toEpochMilli(),
                        reason = ReminderTriggerReason.START,
                    )
            } else if (interval > 0) {
                val windowEndDate = if (reminderWindowCrossesMidnight(windowStart, windowEnd)) {
                    date.plusDays(1)
                } else {
                    date
                }
                var currentDateTime = LocalDateTime.of(date, windowStart)
                val endDateTime = LocalDateTime.of(windowEndDate, windowEnd)
                while (!currentDateTime.isAfter(endDateTime)) {
                    candidates += NextReminderOccurrence(
                        atMillis = currentDateTime.atZone(zoneId).toInstant().toEpochMilli(),
                        reason = if (
                            currentDateTime.toLocalDate() == date &&
                            currentDateTime.toLocalTime() == windowStart
                        ) {
                            ReminderTriggerReason.START
                        } else {
                            ReminderTriggerReason.REPEAT
                        },
                    )
                    currentDateTime = currentDateTime.plusMinutes(interval.toLong())
                }
            }
        }
        return candidates
    }

    private fun isDueOn(
        spec: HabitReminderSpec,
        date: LocalDate,
    ): Boolean {
        return when (spec.frequencyType) {
            HabitFrequencyType.DAILY -> true
            HabitFrequencyType.WEEKLY -> spec.selectedWeekdays.contains(date.dayOfWeek)
            HabitFrequencyType.INTERVAL_DAYS -> {
                val intervalDays = spec.intervalDays ?: return false
                val anchorDate = spec.intervalAnchorDate ?: return false
                if (intervalDays <= 0 || date < anchorDate) {
                    false
                } else {
                    java.time.temporal.ChronoUnit.DAYS.between(anchorDate, date) % intervalDays.toLong() == 0L
                }
            }
            HabitFrequencyType.MONTHLY -> spec.monthlyDays.contains(date.dayOfMonth)
        }
    }

    private fun ReminderTriggerReason.priority(): Int {
        return when (this) {
            ReminderTriggerReason.EXACT -> 0
            ReminderTriggerReason.START -> 1
            ReminderTriggerReason.REPEAT -> 2
        }
    }
}
