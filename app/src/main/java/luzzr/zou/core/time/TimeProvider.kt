package luzzr.zou.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

interface TimeProvider {
    fun nowMillis(): Long
    fun zoneId(): ZoneId

    fun currentDate(): LocalDate = Instant.ofEpochMilli(nowMillis())
        .atZone(zoneId())
        .toLocalDate()
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}
