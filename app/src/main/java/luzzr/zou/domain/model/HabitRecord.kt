package luzzr.zou.domain.model

data class HabitRecord(
    val id: String,
    val habitId: String,
    val recordDate: Long,
    val status: HabitRecordStatus = HabitRecordStatus.PENDING,
    val durationMinutes: Int? = null,
    val stepProgressIds: Set<String> = emptySet(),
    val durationElapsedSeconds: Long = 0L,
    val durationRunningSinceMillis: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
