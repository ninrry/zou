package luzzr.zou.data.repository

import androidx.room.withTransaction
import luzzr.zou.core.markdown.MarkdownImageReferenceParser
import luzzr.zou.core.markdown.MarkdownPreviewTextExtractor
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.ZouDatabase
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
    private val database: ZouDatabase,
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
        val removableMedia = database.withTransaction {
            noteDao.upsertNote(normalizedNote.toEntity())
            removeUnreferencedMedia(
                noteId = normalizedNote.id,
                referencedMediaIds = imageReferenceParser.extractMediaIds(normalizedContent),
            )
        }
        deleteMediaFiles(removableMedia)
    }

    override suspend fun softDeleteNote(noteId: String) {
        val now = timeProvider.nowMillis()
        database.withTransaction {
            noteDao.softDeleteNote(noteId = noteId, deletedAt = now)
            mediaDao.softDeleteMediaByOwner(
                ownerType = ownerType,
                ownerId = noteId,
                updatedAt = now,
            )
        }
    }

    override suspend fun restoreNote(noteId: String) {
        val now = timeProvider.nowMillis()
        database.withTransaction {
            val note = noteDao.getNote(noteId) ?: return@withTransaction
            noteDao.restoreNote(
                noteId = noteId,
                updatedAt = now,
            )
            val referencedMediaIds = imageReferenceParser.extractMediaIds(note.contentMarkdown)
            if (referencedMediaIds.isNotEmpty()) {
                mediaDao.restoreMediaByIdsForOwner(
                    ownerType = ownerType,
                    ownerId = noteId,
                    mediaIds = referencedMediaIds.toList(),
                    updatedAt = now,
                )
            }
        }
    }

    override suspend fun hardDeleteNote(noteId: String) {
        val media = database.withTransaction {
            val ownedMedia = mediaDao.getMediaForOwner(
                ownerType = ownerType,
                ownerId = noteId,
            )
            mediaDao.hardDeleteMediaByOwner(
                ownerType = ownerType,
                ownerId = noteId,
            )
            noteDao.hardDeleteNote(noteId)
            ownedMedia
        }
        deleteMediaFiles(media)
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
        var mediaPersisted = false
        try {
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
            mediaPersisted = true
        } finally {
            if (!mediaPersisted) {
                runCatching { noteImageStorage.deleteImage(storedImage.localPath) }
            }
        }
        return InsertedNoteImage(
            mediaId = storedImage.mediaId,
            markdownReference = "![image](local://media/${storedImage.mediaId})",
            localPath = storedImage.localPath,
            mimeType = storedImage.mimeType,
        )
    }

    override suspend fun cleanupOrphanedMedia() {
        val orphanedMedia = database.withTransaction {
            val noteIds = noteDao.getAllNotes().mapTo(mutableSetOf()) { it.id }
            val orphaned = mediaDao.getAllMedia()
                .filter { media -> media.ownerType == ownerType && media.ownerId !in noteIds }
            deleteMediaRows(orphaned)
            orphaned
        }
        deleteMediaFiles(orphanedMedia)
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
        database.withTransaction {
            noteDao.softDeleteNotes(noteIds = noteIds, deletedAt = now)
            noteIds.forEach { noteId ->
                mediaDao.softDeleteMediaByOwner(
                    ownerType = ownerType,
                    ownerId = noteId,
                    updatedAt = now,
                )
            }
        }
    }

    private suspend fun cleanupUnreferencedMedia(
        noteId: String,
        referencedMediaIds: Set<String>,
    ) {
        val removableMedia = database.withTransaction {
            removeUnreferencedMedia(
                noteId = noteId,
                referencedMediaIds = referencedMediaIds,
            )
        }
        deleteMediaFiles(removableMedia)
    }

    private suspend fun removeUnreferencedMedia(
        noteId: String,
        referencedMediaIds: Set<String>,
    ): List<MediaEntity> {
        val activeMedia = mediaDao.getActiveMediaForOwner(
            ownerType = ownerType,
            ownerId = noteId,
        )
        val removableMedia = activeMedia.filterNot { it.id in referencedMediaIds }
        if (removableMedia.isEmpty()) return emptyList()

        deleteMediaRows(removableMedia)
        return removableMedia
    }

    private suspend fun cleanupOrphanedMedia(noteId: String) {
        val orphanedMedia = database.withTransaction {
            val orphaned = mediaDao.getMediaForOwner(
                ownerType = ownerType,
                ownerId = noteId,
            )
            deleteMediaRows(orphaned)
            orphaned
        }
        deleteMediaFiles(orphanedMedia)
    }

    private suspend fun deleteMediaRows(mediaItems: List<MediaEntity>) {
        mediaItems.forEach { media ->
            mediaDao.hardDeleteMediaById(media.id)
        }
    }

    private fun deleteMediaFiles(mediaItems: List<MediaEntity>) {
        mediaItems.forEach { media ->
            runCatching { noteImageStorage.deleteImage(media.localPath) }
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
