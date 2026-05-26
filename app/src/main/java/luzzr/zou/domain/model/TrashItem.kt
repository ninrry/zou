package luzzr.zou.domain.model

enum class TrashItemType {
    TASK,
    HABIT,
    NOTE,
}

data class TrashItem(
    val id: String,
    val type: TrashItemType,
    val title: String,
    val deletedAt: Long,
    val previewText: String,
)
