package luzzr.zou.domain.model

data class HabitStep(
    val id: String,
    val title: String,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
