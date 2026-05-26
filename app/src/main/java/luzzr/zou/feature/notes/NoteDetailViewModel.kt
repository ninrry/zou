package luzzr.zou.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.usecase.GetNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNoteUseCase: GetNoteUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val noteId: String? = savedStateHandle[NoteRoutes.noteIdArg]

    private val _uiState = MutableStateFlow(
        NoteDetailUiState(
            noteId = noteId,
            isLoading = noteId != null,
            emptyMessage = if (noteId == null) "笔记参数无效" else null,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val currentNoteId = noteId
        if (currentNoteId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    emptyMessage = "笔记不存在或已删除",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emptyMessage = null) }
            val noteDetail = getNoteUseCase(currentNoteId)
            _uiState.update { current ->
                if (noteDetail == null) {
                    current.copy(
                        isLoading = false,
                        emptyMessage = "笔记不存在或已删除",
                    )
                } else {
                    current.copy(
                        noteId = noteDetail.note.id,
                        title = noteDetail.note.title,
                        contentMarkdown = noteDetail.note.contentMarkdown.orEmpty(),
                        updatedAtText = formatUpdatedAt(noteDetail.note.updatedAt),
                        images = noteDetail.images.map { image ->
                            NoteImageUiModel(
                                mediaId = image.mediaId,
                                localPath = image.localPath,
                                mimeType = image.mimeType,
                            )
                        },
                        isLoading = false,
                        emptyMessage = null,
                    )
                }
            }
        }
    }

    private fun formatUpdatedAt(updatedAt: Long): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(
                Instant.ofEpochMilli(updatedAt)
                    .atZone(timeProvider.zoneId()),
            )
    }
}

