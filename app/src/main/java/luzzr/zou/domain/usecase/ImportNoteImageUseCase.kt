package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.InsertedNoteImage
import luzzr.zou.domain.repository.NoteRepository
import javax.inject.Inject

class ImportNoteImageUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(
        noteId: String,
        sourceUri: String,
    ): InsertedNoteImage {
        return repository.importImage(noteId, sourceUri)
    }
}
