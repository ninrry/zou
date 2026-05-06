package luzzr.zou.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["dueAt"]),
        Index(value = ["isUrgent"]),
        Index(value = ["isDeleted"]),
        Index(value = ["status"]),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val contentMarkdown: String? = null,
    val priority: String = "NORMAL",
    val isUrgent: Boolean = false,
    val status: String = "ACTIVE",
    val startRemindAt: Long? = null,
    val remindWindowEndAt: Long? = null,
    val startReminderMinuteOfDay: Int? = null,
    val windowEndMinuteOfDay: Int? = null,
    val dueAt: Long? = null,
    val repeatIntervalMinutes: Int? = null,
    val exactReminderTimesJson: String? = null,
    val allDay: Boolean = false,
    val completionRule: String = "MANUAL",
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val tags: String? = null,
    val archived: Boolean = false,
    val reminderNotificationTitle: String? = null,
    val reminderNotificationBody: String? = null,
)
