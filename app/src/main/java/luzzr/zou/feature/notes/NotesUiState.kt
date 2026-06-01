package luzzr.zou.feature.notes

data class NotesUiState(
    val notes: List<NoteCardUiModel> = emptyList(),
    val emptyTitle: String = "还没有笔记",
    val emptyDescription: String = "点击底部 + 创建笔记。",
    val isLoading: Boolean = true,
)

data class NoteCardUiModel(
    val id: String,
    val title: String,
    val previewText: String,
    val updatedAtText: String,
)

