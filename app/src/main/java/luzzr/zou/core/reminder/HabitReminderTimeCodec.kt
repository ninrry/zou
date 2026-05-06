package luzzr.zou.core.reminder

import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Singleton
class HabitReminderTimeCodec @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(times: List<LocalTime>): String? {
        if (times.isEmpty()) return null
        return json.encodeToString(times.distinct().sorted().map(LocalTime::toString))
    }

    fun decode(rawValue: String?): List<LocalTime> {
        if (rawValue.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(rawValue) }
            .getOrDefault(emptyList())
            .mapNotNull { value -> runCatching { LocalTime.parse(value) }.getOrNull() }
            .distinct()
            .sorted()
    }
}
