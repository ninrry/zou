package luzzr.zou.feature.notes

data class NotesUiState(
    val notes: List<NoteCardUiModel> = emptyList(),
    val emptyTitle: String = "还没有笔记",
    val emptyDescription: String = "点击右下角 + 创建第一条笔记，支持 Markdown 和图片。",
)

data class NoteCardUiModel(
    val id: String,
    val title: String,
    val previewText: String,
    val updatedAtText: String,
)

