package luzzr.zou.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_steps",
    indices = [Index(value = ["habitId"])],
)
data class HabitStepEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val title: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
)
