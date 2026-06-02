package luzzr.zou.feature.notes

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.repository.NoteRepository
import luzzr.zou.domain.usecase.ObserveNotesUseCase
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    observeNotesUseCase: ObserveNotesUseCase,
    private val noteRepository: NoteRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _isSelectMode = MutableStateFlow(false)
    private val _selectedNoteIds = MutableStateFlow(emptySet<String>())

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

    fun bulkExportNotes(context: Context) {
        viewModelScope.launch {
            val selectedIds = _selectedNoteIds.value.toList()
            if (selectedIds.isEmpty()) return@launch

            val appContext = context.applicationContext
            val shareIntent = withContext(Dispatchers.IO) {
                runCatching {
                    buildExportShareIntent(appContext, selectedIds)
                }.getOrNull()
            }

            shareIntent?.let { intent ->
                val chooser = Intent.createChooser(intent, "导出笔记到...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
            exitSelectMode()
        }
    }

    private suspend fun buildExportShareIntent(context: Context, selectedIds: List<String>): Intent? {
        val noteDetails = selectedIds.mapNotNull { noteRepository.getNote(it) }
        if (noteDetails.isEmpty()) return null

        val hasImages = noteDetails.any { it.images.isNotEmpty() }
        val isSingle = noteDetails.size == 1

        return if (isSingle && !hasImages) {
            val detail = noteDetails.first()
            val fileName = sanitizeFileName(detail.note.title) + ".md"
            val tempFile = File(context.cacheDir, fileName)
            tempFile.writeText(detail.note.contentMarkdown.orEmpty())

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile,
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, detail.note.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val zipName = "Zou_Notes_Export_${timeProvider.nowMillis()}.zip"
            val tempZipFile = File(context.cacheDir, zipName)
            val usedEntryNames = mutableSetOf<String>()

            ZipOutputStream(FileOutputStream(tempZipFile)).use { zos ->
                noteDetails.forEach { detail ->
                    val noteTitle = sanitizeFileName(detail.note.title)
                    val mdFileName = uniqueEntryName(
                        preferred = if (isSingle) "index.md" else "$noteTitle.md",
                        usedEntryNames = usedEntryNames,
                    )

                    var content = detail.note.contentMarkdown.orEmpty()
                    detail.images.forEach { image ->
                        val ext = when (image.mimeType) {
                            "image/png" -> "png"
                            "image/gif" -> "gif"
                            else -> "jpg"
                        }
                        val mediaId = image.mediaId
                        val localPlaceholder = "local://media/$mediaId"
                        val relativePath = uniqueEntryName(
                            preferred = "images/$mediaId.$ext",
                            usedEntryNames = usedEntryNames,
                        )
                        content = content.replace(localPlaceholder, relativePath)

                        val imageFile = File(image.localPath)
                        if (imageFile.exists()) {
                            zos.putNextEntry(ZipEntry(relativePath))
                            imageFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }

                    zos.putNextEntry(ZipEntry(mdFileName))
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempZipFile,
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Zou 笔记批量导出")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_").ifBlank { "未命名笔记" }
    }

    private fun uniqueEntryName(preferred: String, usedEntryNames: MutableSet<String>): String {
        if (usedEntryNames.add(preferred)) return preferred

        val dotIndex = preferred.lastIndexOf('.')
        val base = if (dotIndex > 0) preferred.substring(0, dotIndex) else preferred
        val extension = if (dotIndex > 0) preferred.substring(dotIndex) else ""
        var index = 2
        while (true) {
            val candidate = "$base-$index$extension"
            if (usedEntryNames.add(candidate)) return candidate
            index += 1
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
