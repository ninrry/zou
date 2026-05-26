package luzzr.zou.domain.repository

import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskSubTaskAdvanceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface TaskRepository {
    fun observeTasks(includeCompleted: Boolean): Flow<List<Task>>

    fun observeDeletedTasks(): Flow<List<Task>> = emptyFlow()

    suspend fun getTask(taskId: String): Task?

    suspend fun saveTask(task: Task, subTasks: List<SubTask>)

    suspend fun softDeleteTask(taskId: String)

    suspend fun restoreTask(taskId: String) = Unit

    suspend fun hardDeleteTask(taskId: String) = Unit

    suspend fun setTaskCompleted(taskId: String, completed: Boolean)

    suspend fun setSubTaskCompleted(taskId: String, subTaskId: String, completed: Boolean)

    suspend fun advanceTaskSubTask(taskId: String): TaskSubTaskAdvanceResult =
        TaskSubTaskAdvanceResult()
}
