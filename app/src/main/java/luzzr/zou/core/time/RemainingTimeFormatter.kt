package luzzr.zou.core.time

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemainingTimeFormatter @Inject constructor() {

    fun format(dueAtMillis: Long?, nowMillis: Long): String? {
        val target = dueAtMillis ?: return null
        val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(target - nowMillis).coerceAtLeast(0L)
        if (remainingMinutes == 0L) return "即将截止"
        return when {
            remainingMinutes < 60L -> "${remainingMinutes}分钟"
            remainingMinutes < 24L * 60L -> {
                val hours = remainingMinutes / 60L
                val minutes = remainingMinutes % 60L
                if (minutes == 0L) {
                    "${hours}小时"
                } else {
                    "${hours}小时${minutes}分钟"
                }
            }

            else -> {
                val days = remainingMinutes / (24L * 60L)
                val hours = (remainingMinutes % (24L * 60L)) / 60L
                if (hours == 0L) {
                    "${days}天"
                } else {
                    "${days}天${hours}小时"
                }
            }
        }
    }
}
