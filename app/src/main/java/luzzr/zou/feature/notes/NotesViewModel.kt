package luzzr.zou.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.repository.NoteRepository
import luzzr.zou.domain.usecase.ObserveNotesUseCase
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    observeNotesUseCase: ObserveNotesUseCase,
    private val noteRepository: NoteRepository,
    private val noteExportManager: NoteExportManager,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _isSelectMode = MutableStateFlow(false)
    private val _selectedNoteIds = MutableStateFlow(emptySet<String>())
    private val _events = MutableSharedFlow<NotesUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<NotesUiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<NotesUiState> = combine(
        observeNotesUseCase(),
        _isSelectMode,
        _selectedNoteIds,
    ) { notes, isSelect, selectedIds ->
        val noteCards = notes.map { note ->
            NoteCardUiModel(
                id = note.id,
                title = note.title.ifBlank { "无标题笔记" },
                previewText = note.previewText.orEmpty().ifBlank { "暂无正文预览" },
                updatedAtText = formatUpdatedAt(note.updatedAt),
                isPinned = note.isPinned,
            )
        }
        val activeSelectedIds = selectedIds.intersect(noteCards.map { it.id }.toSet())

        NotesUiState(
            notes = noteCards,
            isSelectMode = isSelect && activeSelectedIds.isNotEmpty(),
            selectedNoteIds = activeSelectedIds,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesUiState(),
    )

    fun toggleNoteSelection(noteId: String) {
        val current = _selectedNoteIds.value
        if (current.contains(noteId)) {
            val next = current - noteId
            _selectedNoteIds.value = next
            if (next.isEmpty()) {
                _isSelectMode.value = false
            }
        } else {
            _selectedNoteIds.value = current + noteId
        }
    }

    fun enterSelectMode(initialNoteId: String) {
        _isSelectMode.value = true
        _selectedNoteIds.value = setOf(initialNoteId)
    }

    fun selectAllNotes(noteIds: Set<String>) {
        if (noteIds.isEmpty()) {
            exitSelectMode()
            return
        }
        _isSelectMode.value = true
        _selectedNoteIds.value = noteIds
    }

    fun exitSelectMode() {
        _isSelectMode.value = false
        _selectedNoteIds.value = emptySet()
    }

    fun bulkPinNotes(isPinned: Boolean) {
        viewModelScope.launch {
            val ids = _selectedNoteIds.value.toList()
            if (ids.isNotEmpty()) {
                noteRepository.bulkPinNotes(ids, isPinned)
            }
            exitSelectMode()
        }
    }

    fun bulkSoftDeleteNotes() {
        viewModelScope.launch {
            val ids = _selectedNoteIds.value.toList()
            if (ids.isNotEmpty()) {
                noteRepository.bulkSoftDeleteNotes(ids)
            }
            exitSelectMode()
        }
    }

    fun bulkExportNotes() {
        viewModelScope.launch {
            val selectedIds = _selectedNoteIds.value.toList()
            if (selectedIds.isEmpty()) return@launch

            noteExportManager.export(selectedIds)
                .onSuccess { shareRequest ->
                    _events.emit(NotesUiEvent.ShareNotes(shareRequest))
                    exitSelectMode()
                }
                .onFailure {
                    _events.emit(NotesUiEvent.ShowMessage("导出失败，请稍后重试"))
                }
        }
    }

    private fun formatUpdatedAt(updatedAt: Long): String {
        return DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .format(
                Instant.ofEpochMilli(updatedAt)
                    .atZone(timeProvider.zoneId()),
            )
    }
}
