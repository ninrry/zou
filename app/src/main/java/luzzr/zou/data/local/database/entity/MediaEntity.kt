package luzzr.zou.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    indices = [Index(value = ["ownerType", "ownerId"])],
)
data class MediaEntity(
    @PrimaryKey val id: String,
    val ownerType: String,
    val ownerId: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
)
