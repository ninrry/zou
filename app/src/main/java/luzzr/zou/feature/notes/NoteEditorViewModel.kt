package luzzr.zou.feature.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.domain.model.Note
import luzzr.zou.domain.usecase.DiscardNoteDraftUseCase
import luzzr.zou.domain.usecase.GetNoteUseCase
import luzzr.zou.domain.usecase.ImportNoteImageUseCase
import luzzr.zou.domain.usecase.SaveNoteUseCase
import luzzr.zou.domain.usecase.SoftDeleteNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNoteUseCase: GetNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val softDeleteNoteUseCase: SoftDeleteNoteUseCase,
    private val importNoteImageUseCase: ImportNoteImageUseCase,
    private val discardNoteDraftUseCase: DiscardNoteDraftUseCase,
) : ViewModel() {

    private val existingNoteId: String? = savedStateHandle[NoteRoutes.noteIdArg]
    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            noteId = existingNoteId ?: UUID.randomUUID().toString(),
            isEditing = existingNoteId != null,
            isLoading = existingNoteId != null,
        ),
    )

    val uiState = _uiState.asStateFlow()

    init {
        if (existingNoteId != null) {
            loadNote(existingNoteId)
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update {
            it.copy(
                title = title,
                titleError = null,
                saveErrorMessage = null,
            )
        }
    }

    fun onContentChanged(content: TextFieldValue) {
        _uiState.update {
            it.copy(
                content = content,
                saveErrorMessage = null,
            )
        }
    }

    fun onInsertImage(sourceUri: String) {
        if (_uiState.value.isSaving) return
        val noteId = uiState.value.noteId
        _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }
        viewModelScope.launch {
            runCatching {
                importNoteImageUseCase(
                    noteId = noteId,
                    sourceUri = sourceUri,
                )
            }.onSuccess { insertedImage ->
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        content = insertMarkdownReference(current.content, insertedImage.markdownReference),
                        images = current.images + NoteImageUiModel(
                            mediaId = insertedImage.mediaId,
                            localPath = insertedImage.localPath,
                            mimeType = insertedImage.mimeType,
                        ),
                        saveErrorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = throwable.message ?: "插入图片失败，请重试。",
                    )
                }
            }
        }
    }

    fun validateBeforeSave(): Boolean {
        if (uiState.value.hasMissingContent) {
            _uiState.update { it.copy(saveErrorMessage = uiState.value.loadErrorMessage) }
            return false
        }
        if (uiState.value.title.trim().isEmpty()) {
            _uiState.update { it.copy(titleError = "标题不能为空") }
            return false
        }
        _uiState.update {
            it.copy(
                titleError = null,
                saveErrorMessage = null,
            )
        }
        return true
    }

    fun saveNote(onSaved: () -> Unit = {}) {
        if (_uiState.value.isSaving || !validateBeforeSave()) return
        val current = uiState.value
        _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }
        viewModelScope.launch {
            runCatching {
                saveNoteUseCase(
                    Note(
                        id = current.noteId,
                        title = current.title.trim(),
                        contentMarkdown = current.content.text.ifBlank { null },
                        createdAt = current.createdAt,
                        tags = current.tags,
                        archived = current.archived,
                    ),
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveErrorMessage = null) }
                onSaved()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = throwable.message ?: "保存笔记失败，请重试。",
                    )
                }
            }
        }
    }

    fun onDiscardClicked(): Boolean {
        val noteId = uiState.value.noteId
        viewModelScope.launch {
            runCatching {
                discardNoteDraftUseCase(noteId)
            }
        }
        return true
    }

    fun onDeleteClicked(onDeleted: () -> Unit = {}): Boolean {
        if (_uiState.value.isSaving || !uiState.value.isEditing || uiState.value.hasMissingContent) return false
        _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }
        viewModelScope.launch {
            runCatching {
                softDeleteNoteUseCase(uiState.value.noteId)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveErrorMessage = null) }
                onDeleted()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = throwable.message ?: "删除笔记失败，请重试。",
                    )
                }
            }
        }
        return true
    }

    private fun loadNote(noteId: String) {
        viewModelScope.launch {
            val noteDetail = getNoteUseCase(noteId)
            _uiState.update { current ->
                if (noteDetail == null) {
                    current.copy(
                        isLoading = false,
                        loadErrorMessage = "笔记不存在或已删除。",
                    )
                } else {
                    val content = noteDetail.note.contentMarkdown.orEmpty()
                    NoteEditorUiState(
                        noteId = noteDetail.note.id,
                        isEditing = true,
                        title = noteDetail.note.title,
                        content = TextFieldValue(content, TextRange(content.length)),
                        images = noteDetail.images.map { image ->
                            NoteImageUiModel(
                                mediaId = image.mediaId,
                                localPath = image.localPath,
                                mimeType = image.mimeType,
                            )
                        },
                        createdAt = noteDetail.note.createdAt,
                        tags = noteDetail.note.tags,
                        archived = noteDetail.note.archived,
                        isLoading = false,
                        loadErrorMessage = null,
                    )
                }
            }
        }
    }

    private fun insertMarkdownReference(
        current: TextFieldValue,
        markdownReference: String,
    ): TextFieldValue {
        val selection = current.selection
        val prefix = if (selection.start > 0 && current.text[selection.start - 1] != '\n') "\n" else ""
        val suffix = if (selection.end < current.text.length && current.text[selection.end] != '\n') "\n" else ""
        val insertedText = buildString {
            append(prefix)
            append(markdownReference)
            append(suffix)
        }
        val newText = current.text.replaceRange(selection.start, selection.end, insertedText)
        val cursor = selection.start + insertedText.length
        return TextFieldValue(
            text = newText,
            selection = TextRange(cursor),
        )
    }
}
