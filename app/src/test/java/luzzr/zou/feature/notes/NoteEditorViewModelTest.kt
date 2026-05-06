package luzzr.zou.feature.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import luzzr.zou.MainDispatcherRule
import luzzr.zou.domain.model.InsertedNoteImage
import luzzr.zou.domain.model.Note
import luzzr.zou.domain.model.NoteDetail
import luzzr.zou.domain.repository.NoteRepository
import luzzr.zou.domain.usecase.DiscardNoteDraftUseCase
import luzzr.zou.domain.usecase.GetNoteUseCase
import luzzr.zou.domain.usecase.ImportNoteImageUseCase
import luzzr.zou.domain.usecase.SaveNoteUseCase
import luzzr.zou.domain.usecase.SoftDeleteNoteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun blocksSaveWhenTitleIsBlank() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = createViewModel(repository)

        val valid = viewModel.validateBeforeSave()

        assertFalse(valid)
        assertEquals("标题不能为空", viewModel.uiState.value.titleError)
    }

    @Test
    fun insertsImageMarkdownAtCurrentCursor() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = createViewModel(repository)
        viewModel.onContentChanged(
            TextFieldValue(
                text = "Hello world",
                selection = TextRange(6),
            ),
        )

        viewModel.onInsertImage("content://image")
        advanceUntilIdle()

        assertEquals(
            "Hello \n![image](local://media/media-1)\nworld",
            viewModel.uiState.value.content.text,
        )
        assertEquals(1, viewModel.uiState.value.images.size)
    }

    @Test
    fun discardCallsRepositoryCleanup() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = createViewModel(repository)

        val result = viewModel.onDiscardClicked()
        advanceUntilIdle()

        assertTrue(result)
        assertEquals(listOf(viewModel.uiState.value.noteId), repository.discardedNoteIds)
    }

    @Test
    fun savePersistsTrimmedTitle() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("  Note  ")
        viewModel.onContentChanged(TextFieldValue("正文"))

        viewModel.saveNote()
        advanceUntilIdle()

        assertEquals("Note", repository.savedNotes.single().title)
        assertEquals("正文", repository.savedNotes.single().contentMarkdown)
    }

    @Test
    fun exposesLoadErrorWhenEditingMissingNote() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = NoteEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf(NoteRoutes.noteIdArg to "missing")),
            getNoteUseCase = GetNoteUseCase(repository),
            saveNoteUseCase = SaveNoteUseCase(repository),
            softDeleteNoteUseCase = SoftDeleteNoteUseCase(repository),
            importNoteImageUseCase = ImportNoteImageUseCase(repository),
            discardNoteDraftUseCase = DiscardNoteDraftUseCase(repository),
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasMissingContent)
        assertEquals("笔记不存在或已删除。", viewModel.uiState.value.loadErrorMessage)
    }

    private fun createViewModel(repository: FakeNoteRepository): NoteEditorViewModel {
        return NoteEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            getNoteUseCase = GetNoteUseCase(repository),
            saveNoteUseCase = SaveNoteUseCase(repository),
            softDeleteNoteUseCase = SoftDeleteNoteUseCase(repository),
            importNoteImageUseCase = ImportNoteImageUseCase(repository),
            discardNoteDraftUseCase = DiscardNoteDraftUseCase(repository),
        )
    }

    private class FakeNoteRepository : NoteRepository {
        val notes = MutableStateFlow<List<Note>>(emptyList())
        val savedNotes = mutableListOf<Note>()
        val discardedNoteIds = mutableListOf<String>()

        override fun observeNotes(): Flow<List<Note>> = notes
        override suspend fun getNote(noteId: String): NoteDetail? = null
        override suspend fun saveNote(note: Note) { savedNotes += note }
        override suspend fun softDeleteNote(noteId: String) = Unit
        override suspend fun importImage(noteId: String, sourceUri: String): InsertedNoteImage {
            return InsertedNoteImage(
                mediaId = "media-1",
                markdownReference = "![image](local://media/media-1)",
                localPath = "/tmp/media-1.jpg",
                mimeType = "image/jpeg",
            )
        }
        override suspend fun discardDraft(noteId: String) { discardedNoteIds += noteId }
    }
}
