package luzzr.zou.domain.model

data class SubTask(
    val id: String,
    val title: String,
    val sortOrder: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
