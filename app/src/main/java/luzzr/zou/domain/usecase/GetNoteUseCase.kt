package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.NoteDetail
import luzzr.zou.domain.repository.NoteRepository
import javax.inject.Inject

class GetNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(noteId: String): NoteDetail? = repository.getNote(noteId)
}
