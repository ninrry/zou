package luzzr.zou.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["isDeleted"]),
    ],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val contentMarkdown: String? = null,
    val previewText: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val tags: String? = null,
    val archived: Boolean = false,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
)
