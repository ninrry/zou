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

        val zoneId = timeProvider.zoneId()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        // 日期范围校验：今天已超过结束日期，停止提醒
        if (spec.reminderActiveTo != null && today.isAfter(spec.reminderActiveTo)) {
            return null
        }

        val exactOccurrence = spec.exactReminderTimes
            .firstOrNull { it >= nowMillis && withinDueBoundary(it, spec.dueAt) && withinActiveDateRange(it, spec) }
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

        // 以 reminderActiveFrom 和今天二者较晚者作为循环起始日期
        val loopStartDate = if (spec.reminderActiveFrom != null && spec.reminderActiveFrom.isAfter(nowDate)) {
            spec.reminderActiveFrom
        } else {
            nowDate
        }
        val daysOffset = nowDate.until(loopStartDate).days

        val repeatIntervalMinutes = spec.repeatIntervalMinutes
        repeat(367) { i ->
            val dayOffset = i + daysOffset
            val date = nowDate.plusDays(dayOffset.toLong())
            if (lastDate != null && date.isAfter(lastDate)) return null

            // 日期范围上限校验
            if (spec.reminderActiveTo != null && date.isAfter(spec.reminderActiveTo)) return null

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

    /**
     * 校验某个时间点是否处于提醒激活日期范围内（用于精确提醒时间过滤）。
     */
    private fun withinActiveDateRange(
        candidateMillis: Long,
        spec: TaskReminderSpec,
    ): Boolean {
        if (spec.reminderActiveFrom == null && spec.reminderActiveTo == null) return true
        val zoneId = timeProvider.zoneId()
        val candidateDate = Instant.ofEpochMilli(candidateMillis).atZone(zoneId).toLocalDate()
        if (spec.reminderActiveFrom != null && candidateDate.isBefore(spec.reminderActiveFrom)) return false
        if (spec.reminderActiveTo != null && candidateDate.isAfter(spec.reminderActiveTo)) return false
        return true
    }

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
