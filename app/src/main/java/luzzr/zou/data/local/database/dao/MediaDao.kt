package luzzr.zou.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import luzzr.zou.data.local.database.entity.MediaEntity

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(media: MediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(media: List<MediaEntity>)

    @Query(
        """
        SELECT * FROM media
        WHERE ownerType = :ownerType AND ownerId = :ownerId AND isDeleted = 0
        ORDER BY createdAt ASC
        """,
    )
    suspend fun getActiveMediaForOwner(
        ownerType: String,
        ownerId: String,
    ): List<MediaEntity>

    @Query(
        """
        SELECT * FROM media
        WHERE ownerType = :ownerType AND ownerId = :ownerId
        ORDER BY createdAt ASC
        """,
    )
    suspend fun getMediaForOwner(
        ownerType: String,
        ownerId: String,
    ): List<MediaEntity>

    @Query(
        """
        SELECT * FROM media
        WHERE id = :mediaId
        LIMIT 1
        """,
    )
    suspend fun getMediaById(mediaId: String): MediaEntity?

    @Query("SELECT * FROM media")
    suspend fun getAllMedia(): List<MediaEntity>

    @Query(
        """
        UPDATE media
        SET isDeleted = 1, updatedAt = :updatedAt
        WHERE ownerType = :ownerType AND ownerId = :ownerId AND isDeleted = 0
        """,
    )
    suspend fun softDeleteMediaByOwner(
        ownerType: String,
        ownerId: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE media
        SET isDeleted = 1, updatedAt = :updatedAt
        WHERE id IN (:mediaIds)
        """,
    )
    suspend fun softDeleteMediaByIds(
        mediaIds: List<String>,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE media
        SET isDeleted = 0, updatedAt = :updatedAt
        WHERE ownerType = :ownerType AND ownerId = :ownerId
        """,
    )
    suspend fun restoreMediaByOwner(
        ownerType: String,
        ownerId: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM media WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun hardDeleteMediaByOwner(
        ownerType: String,
        ownerId: String,
    )

    @Query("DELETE FROM media WHERE id = :mediaId")
    suspend fun hardDeleteMediaById(mediaId: String)
}
