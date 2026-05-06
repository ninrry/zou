package luzzr.zou.core.reminder

import luzzr.zou.core.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class TaskReminderCalculator @Inject constructor(
    private val timeProvider: TimeProvider,
) {

    fun calculateNext(
        spec: TaskReminderSpec,
        nowMillis: Long,
    ): NextReminderOccurrence? {
        if (spec.isDeleted || spec.archived || spec.isCompleted || !spec.hasReminderConfiguration) {
            return null
        }

        val exactOccurrence = spec.exactReminderTimes
            .firstOrNull { it >= nowMillis && withinDueBoundary(it, spec.dueAt) }
            ?.let { NextReminderOccurrence(it, ReminderTriggerReason.EXACT) }

        val windowOccurrence = calculateWindowOccurrence(
            spec = spec,
            nowMillis = nowMillis,
        )

        return listOfNotNull(exactOccurrence, windowOccurrence)
            .minWithOrNull(
                compareBy<NextReminderOccurrence>({ it.atMillis }, { it.reason.priority() }),
            )
    }

    private fun calculateWindowOccurrence(
        spec: TaskReminderSpec,
        nowMillis: Long,
    ): NextReminderOccurrence? {
        val startMinute = spec.startReminderMinuteOfDay ?: return null
        val zoneId = timeProvider.zoneId()
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val lastDate = spec.dueAt?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }

        val repeatIntervalMinutes = spec.repeatIntervalMinutes
        repeat(367) { dayOffset ->
            val date = nowDate.plusDays(dayOffset.toLong())
            if (lastDate != null && date.isAfter(lastDate)) return null

            val startAt = date.atMinuteOfDay(startMinute).atZone(zoneId).toInstant().toEpochMilli()
            if (!withinDueBoundary(startAt, spec.dueAt)) return null

            val candidate = if (repeatIntervalMinutes == null) {
                if (startAt >= nowMillis) {
                    NextReminderOccurrence(startAt, ReminderTriggerReason.START)
                } else {
                    null
                }
            } else {
                calculateRepeatOccurrence(
                    nowMillis = nowMillis,
                    startAt = startAt,
                    endMinute = spec.windowEndMinuteOfDay,
                    date = date,
                    repeatIntervalMinutes = repeatIntervalMinutes,
                    dueAt = spec.dueAt,
                )
            }
            if (candidate != null) return candidate
        }
        return null
    }

    private fun calculateRepeatOccurrence(
        nowMillis: Long,
        startAt: Long,
        endMinute: Int?,
        date: LocalDate,
        repeatIntervalMinutes: Int,
        dueAt: Long?,
    ): NextReminderOccurrence? {
        if (endMinute == null || repeatIntervalMinutes <= 0) return null
        if (endMinute < 0 || endMinute > 23 * 60 + 59) return null
        val zoneId = timeProvider.zoneId()
        val endAt = date.atMinuteOfDay(endMinute).atZone(zoneId).toInstant().toEpochMilli()
        if (endAt < startAt) return null
        if (startAt > dueAt ?: Long.MAX_VALUE) return null

        val intervalMillis = repeatIntervalMinutes * 60_000L
        val candidate = if (startAt >= nowMillis) {
            startAt
        } else {
            val elapsed = max(0L, nowMillis - startAt)
            val step = (elapsed / intervalMillis) + 1
            startAt + step * intervalMillis
        }
        if (candidate > endAt || !withinDueBoundary(candidate, dueAt)) return null
        return NextReminderOccurrence(
            atMillis = candidate,
            reason = if (candidate == startAt) ReminderTriggerReason.START else ReminderTriggerReason.REPEAT,
        )
    }

    private fun withinDueBoundary(
        candidate: Long,
        dueAt: Long?,
    ): Boolean = dueAt == null || candidate <= dueAt

    private fun LocalDate.atMinuteOfDay(minuteOfDay: Int): LocalDateTime {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        return atTime(hour, minute)
    }

    private fun ReminderTriggerReason.priority(): Int {
        return when (this) {
            ReminderTriggerReason.EXACT -> 0
            ReminderTriggerReason.START -> 1
            ReminderTriggerReason.REPEAT -> 2
        }
    }
}
