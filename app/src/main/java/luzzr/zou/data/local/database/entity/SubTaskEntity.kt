package luzzr.zou.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sub_tasks",
    indices = [Index(value = ["taskId"])],
)
data class SubTaskEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val sortOrder: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
)
