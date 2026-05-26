package luzzr.zou.core.reminder

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ReminderTimeCodec @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun decode(rawValue: String?): List<Long> {
        if (rawValue.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<Long>>(rawValue)
                .distinct()
                .sorted()
        }.getOrDefault(emptyList())
    }

    fun encode(values: List<Long>): String? {
        val normalized = values
            .distinct()
            .sorted()
        return if (normalized.isEmpty()) {
            null
        } else {
            json.encodeToString(normalized)
        }
    }
}
