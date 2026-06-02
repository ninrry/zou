package luzzr.zou.feature.notes

data class NotesUiState(
    val notes: List<NoteCardUiModel> = emptyList(),
    val emptyTitle: String = "还没有笔记",
    val emptyDescription: String = "点击底部 + 创建笔记。",
    val isLoading: Boolean = true,
    val isSelectMode: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
)

data class NoteCardUiModel(
    val id: String,
    val title: String,
    val previewText: String,
    val updatedAtText: String,
    val isPinned: Boolean = false,
)

sealed interface NotesUiEvent {
    data class ShareNotes(val request: NoteExportShareRequest) : NotesUiEvent
    data class ShowMessage(val message: String) : NotesUiEvent
}
