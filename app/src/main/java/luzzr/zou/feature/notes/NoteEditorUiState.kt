package luzzr.zou.feature.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class NoteEditorUiState(
    val noteId: String,
    val isEditing: Boolean = false,
    val title: String = "",
    val content: TextFieldValue = TextFieldValue("", TextRange(0)),
    val images: List<NoteImageUiModel> = emptyList(),
    val createdAt: Long = 0L,
    val tags: String? = null,
    val archived: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadErrorMessage: String? = null,
    val saveErrorMessage: String? = null,
    val titleError: String? = null,
) {
    val screenTitle: String
        get() = if (isEditing) "编辑笔记" else "新建笔记"

    val saveButtonLabel: String
        get() = if (isEditing) "保存" else "创建"

    val canDelete: Boolean
        get() = isEditing

    val hasMissingContent: Boolean
        get() = isEditing && !isLoading && loadErrorMessage != null
}

data class NoteImageUiModel(
    val mediaId: String,
    val localPath: String,
    val mimeType: String,
)
