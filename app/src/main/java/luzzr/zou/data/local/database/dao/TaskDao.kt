package luzzr.zou.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import luzzr.zou.data.local.database.entity.SubTaskEntity
import luzzr.zou.data.local.database.entity.TaskEntity
import luzzr.zou.data.local.database.model.TaskWithSubTasks
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskDao {
    @Transaction
    @Query(
        """
        SELECT * FROM tasks
        WHERE isDeleted = 0
        """,
    )
    abstract fun observeActiveTasksWithSubTasks(): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query(
        """
        SELECT * FROM tasks
        WHERE isDeleted = 1
        ORDER BY deletedAt DESC, updatedAt DESC
        """,
    )
    abstract fun observeDeletedTasksWithSubTasks(): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    abstract suspend fun getTaskWithSubTasks(taskId: String): TaskWithSubTasks?

    @Query(
        """
        SELECT * FROM tasks
        WHERE id = :taskId AND isDeleted = 0
        LIMIT 1
        """,
    )
    abstract suspend fun getActiveTaskEntity(taskId: String): TaskEntity?

    @Query(
        """
        SELECT * FROM tasks
        WHERE isDeleted = 0
        """,
    )
    abstract suspend fun getActiveTaskEntities(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    abstract suspend fun getAllTaskEntities(): List<TaskEntity>

    @Query("SELECT * FROM sub_tasks")
    abstract suspend fun getAllSubTaskEntities(): List<SubTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSubTasks(subTasks: List<SubTaskEntity>)

    @Query("SELECT * FROM sub_tasks WHERE taskId = :taskId")
    abstract suspend fun getSubTasks(taskId: String): List<SubTaskEntity>

    @Query(
        """
        UPDATE tasks
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :taskId
        """,
    )
    abstract suspend fun updateTaskStatus(
        taskId: String,
        status: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE tasks
        SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE id = :taskId
        """,
    )
    abstract suspend fun markTaskDeleted(
        taskId: String,
        deletedAt: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE sub_tasks
        SET isDeleted = 1, updatedAt = :updatedAt
        WHERE taskId = :taskId
        """,
    )
    abstract suspend fun markAllSubTasksDeleted(
        taskId: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE tasks
        SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt
        WHERE id = :taskId
        """,
    )
    abstract suspend fun restoreTask(
        taskId: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE sub_tasks
        SET isDeleted = 0, updatedAt = :updatedAt
        WHERE taskId = :taskId
        """,
    )
    abstract suspend fun restoreSubTasks(
        taskId: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE sub_tasks
        SET isDeleted = 1, updatedAt = :updatedAt
        WHERE id IN (:subTaskIds)
        """,
    )
    abstract suspend fun markSubTasksDeleted(
        subTaskIds: List<String>,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE sub_tasks
        SET isCompleted = :isCompleted,
            completedAt = :completedAt,
            updatedAt = :updatedAt
        WHERE id = :subTaskId
        """,
    )
    abstract suspend fun updateSubTaskCompletion(
        subTaskId: String,
        isCompleted: Boolean,
        completedAt: Long?,
        updatedAt: Long,
    )

    @Query("DELETE FROM sub_tasks WHERE taskId = :taskId")
    abstract suspend fun deleteSubTasks(taskId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    abstract suspend fun deleteTask(taskId: String)

    @Transaction
    open suspend fun saveTaskWithSubTasks(
        task: TaskEntity,
        subTasks: List<SubTaskEntity>,
        updatedAt: Long,
    ) {
        val existingActiveIds = getSubTasks(task.id)
            .filterNot { it.isDeleted }
            .map { it.id }
            .toSet()
        upsertTask(task)
        if (subTasks.isNotEmpty()) {
            upsertSubTasks(subTasks)
        }
        val incomingIds = subTasks.map { it.id }.toSet()
        val removedIds = existingActiveIds - incomingIds
        if (removedIds.isNotEmpty()) {
            markSubTasksDeleted(
                subTaskIds = removedIds.toList(),
                updatedAt = updatedAt,
            )
        }
    }

    @Transaction
    open suspend fun softDeleteTaskWithSubTasks(
        taskId: String,
        deletedAt: Long,
    ) {
        markTaskDeleted(
            taskId = taskId,
            deletedAt = deletedAt,
            updatedAt = deletedAt,
        )
        markAllSubTasksDeleted(
            taskId = taskId,
            updatedAt = deletedAt,
        )
    }

    @Transaction
    open suspend fun restoreTaskWithSubTasks(
        taskId: String,
        updatedAt: Long,
    ) {
        restoreTask(taskId, updatedAt)
        restoreSubTasks(taskId, updatedAt)
    }

    @Transaction
    open suspend fun hardDeleteTaskWithSubTasks(taskId: String) {
        deleteSubTasks(taskId)
        deleteTask(taskId)
    }
}
