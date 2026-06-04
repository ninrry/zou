package luzzr.zou.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import luzzr.zou.core.markdown.MarkdownImageReferenceParser
import luzzr.zou.core.markdown.MarkdownPreviewTextExtractor
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.ZouDatabase
import luzzr.zou.data.local.database.entity.MediaEntity
import luzzr.zou.data.local.media.NoteImageStorage
import luzzr.zou.data.local.media.StoredNoteImage
import luzzr.zou.domain.model.Note
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteRepositoryImplTest {

    private lateinit var database: ZouDatabase
    private lateinit var repository: NoteRepositoryImpl
    private lateinit var fakeStorage: FakeNoteImageStorage
    private val timeProvider = MutableTimeProvider()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            ZouDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        fakeStorage = FakeNoteImageStorage()
        repository = NoteRepositoryImpl(
            database = database,
            noteDao = database.noteDao(),
            mediaDao = database.mediaDao(),
            timeProvider = timeProvider,
            noteImageStorage = fakeStorage,
            previewTextExtractor = MarkdownPreviewTextExtractor(),
            imageReferenceParser = MarkdownImageReferenceParser(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sortsNotesByRecentEditTime() = runBlocking {
        timeProvider.now = epochMillis(2026, 3, 9, 9, 0)
        repository.saveNote(note(title = "First"))
        timeProvider.now = epochMillis(2026, 3, 9, 10, 0)
        repository.saveNote(note(title = "Second"))

        val titles = repository.observeNotes().first().map { it.title }

        assertEquals(listOf("Second", "First"), titles)
    }

    @Test
    fun importImageCreatesMediaRowAndMarkdownReference() = runBlocking {
        val noteId = UUID.randomUUID().toString()

        val inserted = repository.importImage(noteId, "content://image")

        assertEquals("![image](local://media/media-1)", inserted.markdownReference)
        assertEquals(
            listOf("media-1"),
            database.mediaDao().getActiveMediaForOwner("note", noteId).map { it.id },
        )
    }

    @Test
    fun saveWithoutImageReferencePermanentlyRemovesUnrecoverableMedia() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        val inserted = repository.importImage(noteId, "content://image")
        repository.saveNote(
            note(
                id = noteId,
                title = "With image",
                contentMarkdown = inserted.markdownReference,
            ),
        )

        repository.saveNote(
            note(
                id = noteId,
                title = "Without image",
                contentMarkdown = "Body",
            ),
        )

        val media = database.mediaDao().getMediaById("media-1")
        assertNull(media)
        assertEquals(listOf("/virtual/media-1.jpg"), fakeStorage.deletedPaths)
    }

    @Test
    fun softDeleteHidesNoteAndMedia() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        repository.importImage(noteId, "content://image")
        repository.saveNote(note(id = noteId, title = "Delete me", contentMarkdown = "Body"))

        repository.softDeleteNote(noteId)

        assertTrue(repository.observeNotes().first().isEmpty())
        assertTrue(database.mediaDao().getActiveMediaForOwner("note", noteId).isEmpty())
    }

    @Test
    fun restoreNoteRestoresDeletedMedia() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        val inserted = repository.importImage(noteId, "content://image")
        repository.saveNote(note(id = noteId, title = "Restore me", contentMarkdown = inserted.markdownReference))
        repository.softDeleteNote(noteId)

        repository.restoreNote(noteId)

        val restored = repository.getNote(noteId)
        assertEquals(noteId, restored?.note?.id)
        assertEquals(listOf("media-1"), restored?.images?.map { it.mediaId })
    }

    @Test
    fun restoreNoteDoesNotResurrectPreviouslyRemovedMedia() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        val inserted = repository.importImage(noteId, "content://image")
        repository.saveNote(
            note(
                id = noteId,
                title = "With image",
                contentMarkdown = inserted.markdownReference,
            ),
        )
        repository.saveNote(note(id = noteId, title = "Without image", contentMarkdown = "Body"))
        repository.softDeleteNote(noteId)

        repository.restoreNote(noteId)

        val restored = repository.getNote(noteId)
        assertTrue(restored?.images?.isEmpty() == true)
        assertNull(database.mediaDao().getMediaById("media-1"))
    }

    @Test
    fun restoreMissingNoteDoesNotActivateOrphanedMedia() = runBlocking {
        val missingNoteId = UUID.randomUUID().toString()
        repository.importImage(missingNoteId, "content://image")
        database.mediaDao().softDeleteMediaByOwner(
            ownerType = "note",
            ownerId = missingNoteId,
            updatedAt = timeProvider.nowMillis(),
        )

        repository.restoreNote(missingNoteId)

        assertNull(database.noteDao().getNote(missingNoteId))
        assertTrue(database.mediaDao().getActiveMediaForOwner("note", missingNoteId).isEmpty())
    }

    @Test
    fun restoreNoteOnlyActivatesMediaReferencedByMarkdown() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        val inserted = repository.importImage(noteId, "content://image")
        repository.saveNote(note(id = noteId, title = "Restore exact set", contentMarkdown = inserted.markdownReference))
        database.mediaDao().upsertMedia(
            MediaEntity(
                id = "stale-media",
                ownerType = "note",
                ownerId = noteId,
                localPath = "/virtual/stale-media.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 42L,
                createdAt = timeProvider.nowMillis(),
                updatedAt = timeProvider.nowMillis(),
                isDeleted = true,
            ),
        )
        repository.softDeleteNote(noteId)

        repository.restoreNote(noteId)

        assertEquals(listOf("media-1"), repository.getNote(noteId)?.images?.map { it.mediaId })
        assertTrue(database.mediaDao().getMediaById("stale-media")?.isDeleted == true)
    }

    @Test
    fun restoreNoteCannotActivateReferencedMediaOwnedByAnotherNote() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        val otherNoteId = UUID.randomUUID().toString()
        repository.saveNote(
            note(
                id = noteId,
                title = "Isolated restore",
                contentMarkdown = "![image](local://media/foreign-media)",
            ),
        )
        database.mediaDao().upsertMedia(
            MediaEntity(
                id = "foreign-media",
                ownerType = "note",
                ownerId = otherNoteId,
                localPath = "/virtual/foreign-media.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 42L,
                createdAt = timeProvider.nowMillis(),
                updatedAt = timeProvider.nowMillis(),
                isDeleted = true,
            ),
        )
        repository.softDeleteNote(noteId)

        repository.restoreNote(noteId)

        assertTrue(database.mediaDao().getMediaById("foreign-media")?.isDeleted == true)
        assertTrue(repository.getNote(noteId)?.images?.isEmpty() == true)
    }

    @Test
    fun hardDeleteRemovesNoteAndDeletesFiles() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        repository.importImage(noteId, "content://image")
        repository.saveNote(note(id = noteId, title = "Hard delete", contentMarkdown = "Body"))

        repository.hardDeleteNote(noteId)

        assertTrue(database.noteDao().getNote(noteId) == null)
        assertTrue(database.mediaDao().getAllMedia().none { it.ownerId == noteId })
        assertEquals(setOf("/virtual/media-1.jpg"), fakeStorage.deletedPaths.toSet())
    }

    @Test
    fun softDeleteRollsBackNoteWhenMediaUpdateFails() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        val inserted = repository.importImage(noteId, "content://image")
        repository.saveNote(note(id = noteId, title = "Keep consistent", contentMarkdown = inserted.markdownReference))
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_media_soft_delete
            BEFORE UPDATE OF isDeleted ON media
            WHEN NEW.isDeleted = 1
            BEGIN
                SELECT RAISE(ABORT, 'forced media failure');
            END
            """.trimIndent(),
        )

        val failure = runCatching { repository.softDeleteNote(noteId) }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(noteId, database.noteDao().getActiveNote(noteId)?.id)
        assertEquals(listOf("media-1"), database.mediaDao().getActiveMediaForOwner("note", noteId).map { it.id })
    }

    @Test
    fun importImageDeletesWrittenFileWhenMediaInsertFails() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_media_insert
            BEFORE INSERT ON media
            BEGIN
                SELECT RAISE(ABORT, 'forced media insert failure');
            END
            """.trimIndent(),
        )

        val failure = runCatching {
            repository.importImage(UUID.randomUUID().toString(), "content://image")
        }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(listOf("/virtual/media-1.jpg"), fakeStorage.deletedPaths)
        assertTrue(database.mediaDao().getAllMedia().isEmpty())
    }

    @Test
    fun hardDeleteKeepsFilesWhenDatabaseDeleteFails() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        val inserted = repository.importImage(noteId, "content://image")
        repository.saveNote(note(id = noteId, title = "Keep on failure", contentMarkdown = inserted.markdownReference))
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_note_delete
            BEFORE DELETE ON notes
            BEGIN
                SELECT RAISE(ABORT, 'forced note delete failure');
            END
            """.trimIndent(),
        )

        val failure = runCatching { repository.hardDeleteNote(noteId) }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(noteId, database.noteDao().getNote(noteId)?.id)
        assertEquals(listOf("media-1"), database.mediaDao().getMediaForOwner("note", noteId).map { it.id })
        assertTrue(fakeStorage.deletedPaths.isEmpty())
    }

    @Test
    fun discardDraftHardDeletesOrphanedMediaForMissingNote() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        repository.importImage(noteId, "content://image")

        repository.discardDraft(noteId)

        assertTrue(database.mediaDao().getMediaForOwner("note", noteId).isEmpty())
        assertEquals(listOf("/virtual/media-1.jpg"), fakeStorage.deletedPaths)
    }

    @Test
    fun getNoteCleansOrphanedMediaWhenNoteDoesNotExist() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        repository.importImage(noteId, "content://image")

        val note = repository.getNote(noteId)

        assertNull(note)
        assertTrue(database.mediaDao().getMediaForOwner("note", noteId).isEmpty())
        assertEquals(listOf("/virtual/media-1.jpg"), fakeStorage.deletedPaths)
    }

    @Test
    fun cleanupOrphanedMediaSweepsUnreachableDraftMedia() = runBlocking {
        val firstDraftId = UUID.randomUUID().toString()
        val secondDraftId = UUID.randomUUID().toString()
        repository.importImage(firstDraftId, "content://image")
        repository.importImage(secondDraftId, "content://image")
        repository.saveNote(
            note(
                id = secondDraftId,
                title = "Kept note",
                contentMarkdown = "![image](local://media/media-2)",
            ),
        )

        repository.cleanupOrphanedMedia()

        assertTrue(database.mediaDao().getMediaForOwner("note", firstDraftId).isEmpty())
        assertEquals(listOf("media-2"), database.mediaDao().getMediaForOwner("note", secondDraftId).map { it.id })
        assertEquals(listOf("/virtual/media-1.jpg"), fakeStorage.deletedPaths)
    }

    private fun note(
        id: String = UUID.randomUUID().toString(),
        title: String,
        contentMarkdown: String? = null,
    ): Note {
        return Note(
            id = id,
            title = title,
            contentMarkdown = contentMarkdown,
        )
    }

    private fun epochMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.of("Asia/Singapore"))
            .toInstant()
            .toEpochMilli()
    }

    private class MutableTimeProvider : TimeProvider {
        var now: Long = 0L

        override fun nowMillis(): Long = now

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }

    private class FakeNoteImageStorage : NoteImageStorage {
        private var nextId = 1
        val deletedPaths = mutableListOf<String>()

        override suspend fun importImage(noteId: String, sourceUri: String): StoredNoteImage {
            val mediaId = "media-${nextId++}"
            return StoredNoteImage(
                mediaId = mediaId,
                localPath = "/virtual/$mediaId.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 42L,
            )
        }

        override fun deleteImage(localPath: String) {
            deletedPaths += localPath
        }
    }
}
