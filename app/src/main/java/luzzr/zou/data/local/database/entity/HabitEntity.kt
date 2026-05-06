package luzzr.zou.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [Index(value = ["isDeleted"])],
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val contentMarkdown: String? = null,
    val frequencyType: String,
    val frequencyValueJson: String? = null,
    val remindWindowStart: String? = null,
    val remindWindowEnd: String? = null,
    val repeatIntervalMinutes: Int? = null,
    val exactReminderTimesJson: String? = null,
    val checkInMode: String = "CHECK",
    val targetDurationMinutes: Int? = null,
    val streakCountCache: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val tags: String? = null,
    val archived: Boolean = false,
    val reminderNotificationTitle: String? = null,
    val reminderNotificationBody: String? = null,
)
