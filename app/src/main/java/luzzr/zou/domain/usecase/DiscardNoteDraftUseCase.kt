package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.NoteRepository
import javax.inject.Inject

class DiscardNoteDraftUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(noteId: String) = repository.discardDraft(noteId)
}
