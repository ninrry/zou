package luzzr.zou.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import luzzr.zou.data.local.database.entity.HabitEntity
import luzzr.zou.data.local.database.entity.HabitRecordEntity
import luzzr.zou.data.local.database.entity.HabitStepEntity
import luzzr.zou.data.local.database.model.HabitWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HabitDao {
    @Transaction
    @Query(
        """
        SELECT * FROM habits
        WHERE (:includeDeleted = 1 OR isDeleted = 0)
        ORDER BY updatedAt DESC
        """,
    )
    abstract fun observeHabitsWithSteps(includeDeleted: Boolean): Flow<List<HabitWithSteps>>

    @Query(
        """
        SELECT * FROM habits
        WHERE id = :habitId AND isDeleted = 0
        LIMIT 1
        """,
    )
    abstract suspend fun getActiveHabitEntity(habitId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE isDeleted = 0")
    abstract suspend fun getActiveHabitEntities(): List<HabitEntity>

    @Query("SELECT * FROM habits")
    abstract suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habit_steps")
    abstract suspend fun getAllHabitSteps(): List<HabitStepEntity>

    @Query("SELECT * FROM habit_records")
    abstract suspend fun getAllHabitRecords(): List<HabitRecordEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM habits
        WHERE id = :habitId AND (:includeDeleted = 1 OR isDeleted = 0)
        LIMIT 1
        """,
    )
    abstract suspend fun getHabitWithSteps(
        habitId: String,
        includeDeleted: Boolean,
    ): HabitWithSteps?

    @Query("SELECT * FROM habit_records WHERE recordDate = :recordDate")
    abstract fun observeRecordsForDate(recordDate: Long): Flow<List<HabitRecordEntity>>

    @Query(
        """
        SELECT * FROM habit_records
        WHERE habitId = :habitId AND recordDate = :recordDate
        LIMIT 1
        """,
    )
    abstract suspend fun getHabitRecordForDate(
        habitId: String,
        recordDate: Long,
    ): HabitRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertHabitSteps(steps: List<HabitStepEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertHabitRecord(record: HabitRecordEntity)

    @Query("SELECT * FROM habit_steps WHERE habitId = :habitId")
    abstract suspend fun getHabitSteps(habitId: String): List<HabitStepEntity>

    @Query(
        """
        UPDATE habit_steps
        SET isDeleted = 1, updatedAt = :updatedAt
        WHERE id IN (:stepIds)
        """,
    )
    abstract suspend fun markHabitStepsDeleted(
        stepIds: List<String>,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE habits
        SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt
        WHERE id = :habitId
        """,
    )
    abstract suspend fun softDeleteHabit(
        habitId: String,
        deletedAt: Long,
    )

    @Query(
        """
        UPDATE habits
        SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt
        WHERE id = :habitId
        """,
    )
    abstract suspend fun restoreHabit(
        habitId: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM habit_records WHERE habitId = :habitId")
    abstract suspend fun deleteHabitRecords(habitId: String)

    @Query("DELETE FROM habit_steps WHERE habitId = :habitId")
    abstract suspend fun deleteHabitSteps(habitId: String)

    @Query("DELETE FROM habits WHERE id = :habitId")
    abstract suspend fun deleteHabit(habitId: String)

    @Transaction
    open suspend fun saveHabitWithSteps(
        habit: HabitEntity,
        steps: List<HabitStepEntity>,
        updatedAt: Long,
    ) {
        val existingActiveIds = getHabitSteps(habit.id)
            .filterNot { it.isDeleted }
            .map { it.id }
            .toSet()
        upsertHabit(habit)
        if (steps.isNotEmpty()) {
            upsertHabitSteps(steps)
        }
        val incomingIds = steps.map { it.id }.toSet()
        val removedIds = existingActiveIds - incomingIds
        if (removedIds.isNotEmpty()) {
            markHabitStepsDeleted(
                stepIds = removedIds.toList(),
                updatedAt = updatedAt,
            )
        }
    }

    @Transaction
    open suspend fun hardDeleteHabitTree(habitId: String) {
        deleteHabitRecords(habitId)
        deleteHabitSteps(habitId)
        deleteHabit(habitId)
    }
}
