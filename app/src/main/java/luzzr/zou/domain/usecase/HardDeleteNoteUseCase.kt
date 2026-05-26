package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.NoteRepository
import javax.inject.Inject

class HardDeleteNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(noteId: String) {
        noteRepository.hardDeleteNote(noteId)
    }
}
