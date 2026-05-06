package luzzr.zou.feature.notes

data class NoteDetailUiState(
    val noteId: String? = null,
    val title: String = "",
    val contentMarkdown: String = "",
    val updatedAtText: String = "",
    val images: List<NoteImageUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val emptyMessage: String? = null,
) {
    val canEdit: Boolean
        get() = !isLoading && emptyMessage == null && noteId != null
}

