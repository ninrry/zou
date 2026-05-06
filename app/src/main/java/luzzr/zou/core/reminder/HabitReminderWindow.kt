package luzzr.zou.core.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal fun reminderWindowCrossesMidnight(
    windowStart: LocalTime?,
    windowEnd: LocalTime?,
): Boolean {
    return windowStart != null && windowEnd != null && windowEnd.isBefore(windowStart)
}

internal fun habitReminderOccurrenceBaseDate(
    occurrenceMillis: Long,
    zoneId: ZoneId,
    windowStart: LocalTime?,
    windowEnd: LocalTime?,
): LocalDate {
    val occurrenceDateTime = Instant.ofEpochMilli(occurrenceMillis)
        .atZone(zoneId)
        .toLocalDateTime()
    return if (
        reminderWindowCrossesMidnight(windowStart, windowEnd) &&
        windowStart != null &&
        occurrenceDateTime.toLocalTime().isBefore(windowStart)
    ) {
        occurrenceDateTime.toLocalDate().minusDays(1)
    } else {
        occurrenceDateTime.toLocalDate()
    }
}

internal fun habitReminderBoundaryMillis(
    occurrenceMillis: Long,
    zoneId: ZoneId,
    windowStart: LocalTime?,
    windowEnd: LocalTime?,
): Long {
    val endTime = windowEnd ?: return Long.MIN_VALUE
    val baseDate = habitReminderOccurrenceBaseDate(
        occurrenceMillis = occurrenceMillis,
        zoneId = zoneId,
        windowStart = windowStart,
        windowEnd = windowEnd,
    )
    val boundaryDate = if (reminderWindowCrossesMidnight(windowStart, windowEnd)) {
        baseDate.plusDays(1)
    } else {
        baseDate
    }
    return boundaryDate.atTime(endTime)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}
