package luzzr.zou.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_records",
    indices = [Index(value = ["habitId", "recordDate"])],
)
data class HabitRecordEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val recordDate: Long,
    val status: String = "PENDING",
    val durationMinutes: Int? = null,
    val stepProgressJson: String? = null,
    val durationElapsedSeconds: Long = 0L,
    val durationRunningSinceMillis: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
