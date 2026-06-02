package luzzr.zou.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import luzzr.zou.data.local.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
        ORDER BY isPinned DESC, pinnedAt DESC, updatedAt DESC
        """,
    )
    fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 1
        ORDER BY deletedAt DESC, updatedAt DESC
        """,
    )
    fun observeDeletedNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE id = :noteId AND isDeleted = 0
        LIMIT 1
        """,
    )
    suspend fun getActiveNote(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNote(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Query(
        """
        UPDATE notes
        SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt
        WHERE id = :noteId
        """,
    )
    suspend fun softDeleteNote(
        noteId: String,
        deletedAt: Long,
    )

    @Query(
        """
        UPDATE notes
        SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt
        WHERE id = :noteId
        """,
    )
    suspend fun restoreNote(
        noteId: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun hardDeleteNote(noteId: String)

    @Query(
        """
        UPDATE notes
        SET isPinned = :isPinned, pinnedAt = :pinnedAt, updatedAt = :updatedAt
        WHERE id IN (:noteIds)
        """,
    )
    suspend fun updatePinnedStatus(
        noteIds: List<String>,
        isPinned: Boolean,
        pinnedAt: Long?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE notes
        SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt
        WHERE id IN (:noteIds)
        """,
    )
    suspend fun softDeleteNotes(
        noteIds: List<String>,
        deletedAt: Long,
    )
}
