package luzzr.zou.domain.model

data class Note(
    val id: String,
    val title: String,
    val contentMarkdown: String? = null,
    val previewText: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastOpenedAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val tags: String? = null,
    val archived: Boolean = false,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
)
