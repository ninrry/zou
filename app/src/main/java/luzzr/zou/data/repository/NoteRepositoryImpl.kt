package luzzr.zou.data.repository

import luzzr.zou.core.markdown.MarkdownImageReferenceParser
import luzzr.zou.core.markdown.MarkdownPreviewTextExtractor
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.dao.MediaDao
import luzzr.zou.data.local.database.dao.NoteDao
import luzzr.zou.data.local.database.entity.MediaEntity
import luzzr.zou.data.local.database.entity.NoteEntity
import luzzr.zou.data.local.media.NoteImageStorage
import luzzr.zou.domain.model.InsertedNoteImage
import luzzr.zou.domain.model.Note
import luzzr.zou.domain.model.NoteDetail
import luzzr.zou.domain.model.NoteImage
import luzzr.zou.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val mediaDao: MediaDao,
    private val timeProvider: TimeProvider,
    private val noteImageStorage: NoteImageStorage,
    private val previewTextExtractor: MarkdownPreviewTextExtractor,
    private val imageReferenceParser: MarkdownImageReferenceParser,
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> {
        return noteDao.observeActiveNotes().map { notes ->
            notes.map { entity -> entity.toDomain() }
        }
    }

    override fun observeDeletedNotes(): Flow<List<Note>> {
        return noteDao.observeDeletedNotes().map { notes ->
            notes.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun getNote(noteId: String): NoteDetail? {
        val note = noteDao.getActiveNote(noteId)
        if (note == null) {
            if (noteDao.getNote(noteId) == null) {
                cleanupOrphanedMedia(noteId)
            }
            return null
        }
        return NoteDetail(
            note = note.toDomain(),
            images = mediaDao.getActiveMediaForOwner(ownerType = ownerType, ownerId = noteId)
                .map { media -> media.toDomain() },
        )
    }

    override suspend fun saveNote(note: Note) {
        val now = timeProvider.nowMillis()
        val normalizedContent = note.contentMarkdown?.trimEnd()
        val normalizedNote = note.copy(
            contentMarkdown = normalizedContent,
            previewText = previewTextExtractor.extractPreview(normalizedContent),
            createdAt = if (note.createdAt == 0L) now else note.createdAt,
            updatedAt = now,
        )
        noteDao.upsertNote(normalizedNote.toEntity())
        cleanupUnreferencedMedia(
            noteId = normalizedNote.id,
            referencedMediaIds = imageReferenceParser.extractMediaIds(normalizedContent),
            updatedAt = now,
        )
    }

    override suspend fun softDeleteNote(noteId: String) {
        val now = timeProvider.nowMillis()
        noteDao.softDeleteNote(noteId = noteId, deletedAt = now)
        mediaDao.softDeleteMediaByOwner(
            ownerType = ownerType,
            ownerId = noteId,
            updatedAt = now,
        )
    }

    override suspend fun restoreNote(noteId: String) {
        val now = timeProvider.nowMillis()
        noteDao.restoreNote(
            noteId = noteId,
            updatedAt = now,
        )
        mediaDao.restoreMediaByOwner(
            ownerType = ownerType,
            ownerId = noteId,
            updatedAt = now,
        )
    }

    override suspend fun hardDeleteNote(noteId: String) {
        val media = mediaDao.getMediaForOwner(
            ownerType = ownerType,
            ownerId = noteId,
        )
        media.forEach { item ->
            runCatching { noteImageStorage.deleteImage(item.localPath) }
        }
        mediaDao.hardDeleteMediaByOwner(
            ownerType = ownerType,
            ownerId = noteId,
        )
        noteDao.hardDeleteNote(noteId)
    }

    override suspend fun importImage(
        noteId: String,
        sourceUri: String,
    ): InsertedNoteImage {
        val now = timeProvider.nowMillis()
        val storedImage = noteImageStorage.importImage(
            noteId = noteId,
            sourceUri = sourceUri,
        )
        mediaDao.upsertMedia(
            MediaEntity(
                id = storedImage.mediaId,
                ownerType = ownerType,
                ownerId = noteId,
                localPath = storedImage.localPath,
                mimeType = storedImage.mimeType,
                sizeBytes = storedImage.sizeBytes,
                createdAt = now,
                updatedAt = now,
                isDeleted = false,
            ),
        )
        return InsertedNoteImage(
            mediaId = storedImage.mediaId,
            markdownReference = "![image](local://media/${storedImage.mediaId})",
            localPath = storedImage.localPath,
            mimeType = storedImage.mimeType,
        )
    }

    override suspend fun cleanupOrphanedMedia() {
        val noteIds = noteDao.getAllNotes().mapTo(mutableSetOf()) { it.id }
        val orphanedMedia = mediaDao.getAllMedia()
            .filter { media -> media.ownerType == ownerType && media.ownerId !in noteIds }

        hardDeleteMedia(orphanedMedia)
    }

    override suspend fun discardDraft(noteId: String) {
        val persistedNote = noteDao.getNote(noteId)
        if (persistedNote == null) {
            cleanupOrphanedMedia(noteId)
            return
        }
        val persistedMarkdown = persistedNote.contentMarkdown
        val referencedMediaIds = imageReferenceParser.extractMediaIds(persistedMarkdown)
        cleanupUnreferencedMedia(
            noteId = noteId,
            referencedMediaIds = referencedMediaIds,
            updatedAt = timeProvider.nowMillis(),
        )
    }

    override suspend fun bulkPinNotes(noteIds: List<String>, isPinned: Boolean) {
        if (noteIds.isEmpty()) return
        val now = timeProvider.nowMillis()
        noteDao.updatePinnedStatus(
            noteIds = noteIds,
            isPinned = isPinned,
            pinnedAt = if (isPinned) now else null,
            updatedAt = now,
        )
    }

    override suspend fun bulkSoftDeleteNotes(noteIds: List<String>) {
        if (noteIds.isEmpty()) return
        val now = timeProvider.nowMillis()
        noteDao.softDeleteNotes(noteIds = noteIds, deletedAt = now)
        noteIds.forEach { noteId ->
            mediaDao.softDeleteMediaByOwner(
                ownerType = ownerType,
                ownerId = noteId,
                updatedAt = now,
            )
        }
    }

    private suspend fun cleanupUnreferencedMedia(
        noteId: String,
        referencedMediaIds: Set<String>,
        updatedAt: Long,
    ) {
        val activeMedia = mediaDao.getActiveMediaForOwner(
            ownerType = ownerType,
            ownerId = noteId,
        )
        val removableMedia = activeMedia.filterNot { it.id in referencedMediaIds }
        if (removableMedia.isEmpty()) return

        mediaDao.softDeleteMediaByIds(
            mediaIds = removableMedia.map { it.id },
            updatedAt = updatedAt,
        )
        removableMedia.forEach { media ->
            runCatching { noteImageStorage.deleteImage(media.localPath) }
        }
    }

    private suspend fun cleanupOrphanedMedia(noteId: String) {
        val orphanedMedia = mediaDao.getMediaForOwner(
            ownerType = ownerType,
            ownerId = noteId,
        )
        hardDeleteMedia(orphanedMedia)
    }

    private suspend fun hardDeleteMedia(mediaItems: List<MediaEntity>) {
        if (mediaItems.isEmpty()) return

        mediaItems.forEach { media ->
            runCatching { noteImageStorage.deleteImage(media.localPath) }
            mediaDao.hardDeleteMediaById(media.id)
        }
    }

    private fun NoteEntity.toDomain(): Note {
        return Note(
            id = id,
            title = title,
            contentMarkdown = contentMarkdown,
            previewText = previewText,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastOpenedAt = lastOpenedAt,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            tags = tags,
            archived = archived,
            isPinned = isPinned,
            pinnedAt = pinnedAt,
        )
    }

    private fun MediaEntity.toDomain(): NoteImage {
        return NoteImage(
            mediaId = id,
            ownerId = ownerId,
            localPath = localPath,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            isDeleted = isDeleted,
        )
    }

    private fun Note.toEntity(): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            contentMarkdown = contentMarkdown,
            previewText = previewText,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastOpenedAt = lastOpenedAt,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            tags = tags,
            archived = archived,
            isPinned = isPinned,
            pinnedAt = pinnedAt,
        )
    }

    private companion object {
        const val ownerType = "note"
    }
}
