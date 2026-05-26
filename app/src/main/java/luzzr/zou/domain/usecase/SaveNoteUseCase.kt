package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.Note
import luzzr.zou.domain.repository.NoteRepository
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(note: Note) = repository.saveNote(note)
}
