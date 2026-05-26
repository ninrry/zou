package luzzr.zou.data.repository

import luzzr.zou.core.reminder.ReminderDispatchQueue
import luzzr.zou.core.reminder.ReminderTimeCodec
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.dao.TaskDao
import luzzr.zou.data.local.database.entity.SubTaskEntity
import luzzr.zou.data.local.database.entity.TaskEntity
import luzzr.zou.data.local.database.model.TaskWithSubTasks
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import luzzr.zou.domain.model.TaskSubTaskAdvanceResult
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.repository.TaskRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val reminderDispatchQueue: ReminderDispatchQueue,
    private val reminderTimeCodec: ReminderTimeCodec,
    private val timeProvider: TimeProvider,
) : TaskRepository {

    override fun observeTasks(includeCompleted: Boolean): Flow<List<Task>> {
        return taskDao.observeActiveTasksWithSubTasks()
            .map { taskGraphs ->
                val now = timeProvider.nowMillis()
                taskGraphs
                    .map(::mapTask)
                    .let { tasks ->
                        if (includeCompleted) {
                            tasks
                                .filterNot { it.isOverdue(now) }
                                .sortedWith(taskComparator(now))
                        } else {
                            tasks
                                .filterNot { it.status == TaskStatus.COMPLETED }
                                .filterNot { it.isOverdue(now) }
                                .sortedWith(taskComparator(now))
                        }
                    }
            }
    }

    override fun observeDeletedTasks(): Flow<List<Task>> {
        return taskDao.observeDeletedTasksWithSubTasks()
            .map { taskGraphs ->
                taskGraphs.map(::mapTask)
                    .sortedByDescending { it.deletedAt ?: it.updatedAt }
            }
    }

    override suspend fun getTask(taskId: String): Task? {
        return taskDao.getTaskWithSubTasks(taskId)?.let(::mapTask)
    }

    override suspend fun saveTask(task: Task, subTasks: List<SubTask>) {
        val now = timeProvider.nowMillis()
        val cleanedSubTasks = subTasks
            .filter { it.title.isNotBlank() }
            .mapIndexed { index, subTask ->
                subTask.copy(
                    sortOrder = index,
                    createdAt = if (subTask.createdAt == 0L) now else subTask.createdAt,
                    updatedAt = now,
                )
            }
        val normalizedTask = task.copy(
            createdAt = if (task.createdAt == 0L) now else task.createdAt,
            updatedAt = now,
            status = resolveTaskStatus(task, cleanedSubTasks),
        )
        taskDao.saveTaskWithSubTasks(
            task = normalizedTask.toEntity(),
            subTasks = cleanedSubTasks.map { it.toEntity(taskId = normalizedTask.id) },
            updatedAt = now,
        )
        reminderDispatchQueue.scheduleTask(normalizedTask.id)
    }

    override suspend fun softDeleteTask(taskId: String) {
        taskDao.softDeleteTaskWithSubTasks(
            taskId = taskId,
            deletedAt = timeProvider.nowMillis(),
        )
        reminderDispatchQueue.cancelTask(taskId)
    }

    override suspend fun restoreTask(taskId: String) {
        val now = timeProvider.nowMillis()
        taskDao.restoreTaskWithSubTasks(
            taskId = taskId,
            updatedAt = now,
        )
        reminderDispatchQueue.scheduleTask(taskId)
    }

    override suspend fun hardDeleteTask(taskId: String) {
        taskDao.hardDeleteTaskWithSubTasks(taskId)
        reminderDispatchQueue.cancelTask(taskId)
    }

    override suspend fun setTaskCompleted(taskId: String, completed: Boolean) {
        val task = taskDao.getTaskWithSubTasks(taskId)?.let(::mapTask) ?: return
        val activeSubTasks = task.subTasks
        if (task.completionRule == TaskCompletionRule.AUTO_ALL_SUBTASKS && activeSubTasks.isNotEmpty()) {
            return
        }
        taskDao.updateTaskStatus(
            taskId = taskId,
            status = if (completed) TaskStatus.COMPLETED.name else TaskStatus.ACTIVE.name,
            updatedAt = timeProvider.nowMillis(),
        )
        reminderDispatchQueue.scheduleTask(taskId)
    }

    override suspend fun setSubTaskCompleted(taskId: String, subTaskId: String, completed: Boolean) {
        val task = taskDao.getTaskWithSubTasks(taskId)?.let(::mapTask) ?: return
        val now = timeProvider.nowMillis()
        if (task.subTasks.none { it.id == subTaskId }) return
        taskDao.updateSubTaskCompletion(
            subTaskId = subTaskId,
            isCompleted = completed,
            completedAt = if (completed) now else null,
            updatedAt = now,
        )
        if (task.completionRule == TaskCompletionRule.AUTO_ALL_SUBTASKS && task.subTasks.isNotEmpty()) {
            val updatedSubTasks = task.subTasks.map { subTask ->
                if (subTask.id == subTaskId) {
                    subTask.copy(
                        isCompleted = completed,
                        completedAt = if (completed) now else null,
                        updatedAt = now,
                    )
                } else {
                    subTask
                }
            }
            taskDao.updateTaskStatus(
                taskId = taskId,
                status = if (updatedSubTasks.all { it.isCompleted }) {
                    TaskStatus.COMPLETED.name
                } else {
                    TaskStatus.ACTIVE.name
                },
                updatedAt = now,
            )
        }
        reminderDispatchQueue.scheduleTask(taskId)
    }

    override suspend fun advanceTaskSubTask(taskId: String): TaskSubTaskAdvanceResult {
        val task = taskDao.getTaskWithSubTasks(taskId)?.let(::mapTask) ?: return TaskSubTaskAdvanceResult()
        if (task.status == TaskStatus.COMPLETED) {
            return TaskSubTaskAdvanceResult(
                progressedSubTaskId = null,
                completedCount = task.subTasks.count { it.isCompleted },
                totalCount = task.subTasks.size,
                taskCompleted = true,
            )
        }
        val nextSubTask = task.subTasks
            .sortedBy { it.sortOrder }
            .firstOrNull { !it.isCompleted }
            ?: return TaskSubTaskAdvanceResult(
                progressedSubTaskId = null,
                completedCount = task.subTasks.count { it.isCompleted },
                totalCount = task.subTasks.size,
                taskCompleted = false,
            )

        val now = timeProvider.nowMillis()
        taskDao.updateSubTaskCompletion(
            subTaskId = nextSubTask.id,
            isCompleted = true,
            completedAt = now,
            updatedAt = now,
        )

        val completedCount = task.subTasks.count { it.isCompleted } + 1
        val taskCompleted = completedCount >= task.subTasks.size
        if (task.completionRule == TaskCompletionRule.AUTO_ALL_SUBTASKS && task.subTasks.isNotEmpty()) {
            taskDao.updateTaskStatus(
                taskId = taskId,
                status = if (taskCompleted) TaskStatus.COMPLETED.name else TaskStatus.ACTIVE.name,
                updatedAt = now,
            )
        }
        reminderDispatchQueue.scheduleTask(taskId)
        return TaskSubTaskAdvanceResult(
            progressedSubTaskId = nextSubTask.id,
            completedCount = completedCount,
            totalCount = task.subTasks.size,
            taskCompleted = taskCompleted,
        )
    }

    private fun mapTask(taskWithSubTasks: TaskWithSubTasks): Task {
        val activeSubTasks = taskWithSubTasks.subTasks
            .filterNot { it.isDeleted }
            .sortedBy { it.sortOrder }
            .map { subTask ->
                SubTask(
                    id = subTask.id,
                    title = subTask.title,
                    sortOrder = subTask.sortOrder,
                    isCompleted = subTask.isCompleted,
                    completedAt = subTask.completedAt,
                    createdAt = subTask.createdAt,
                    updatedAt = subTask.updatedAt,
                )
            }
        return Task(
            id = taskWithSubTasks.task.id,
            title = taskWithSubTasks.task.title,
            contentMarkdown = taskWithSubTasks.task.contentMarkdown,
            priority = taskWithSubTasks.task.priority.toTaskPriority(),
            isUrgent = taskWithSubTasks.task.isUrgent,
            status = taskWithSubTasks.task.status.toTaskStatus(),
            startRemindAt = taskWithSubTasks.task.startRemindAt,
            remindWindowEndAt = taskWithSubTasks.task.remindWindowEndAt,
            startReminderMinuteOfDay = taskWithSubTasks.task.startReminderMinuteOfDay,
            windowEndMinuteOfDay = taskWithSubTasks.task.windowEndMinuteOfDay,
            dueAt = taskWithSubTasks.task.dueAt,
            repeatIntervalMinutes = taskWithSubTasks.task.repeatIntervalMinutes,
            exactReminderTimes = reminderTimeCodec.decode(taskWithSubTasks.task.exactReminderTimesJson),
            allDay = taskWithSubTasks.task.allDay,
            completionRule = taskWithSubTasks.task.completionRule.toTaskCompletionRule(),
            createdAt = taskWithSubTasks.task.createdAt,
            updatedAt = taskWithSubTasks.task.updatedAt,
            isDeleted = taskWithSubTasks.task.isDeleted,
            deletedAt = taskWithSubTasks.task.deletedAt,
            tags = taskWithSubTasks.task.tags,
            archived = taskWithSubTasks.task.archived,
            subTasks = activeSubTasks,
            reminderNotificationTitle = taskWithSubTasks.task.reminderNotificationTitle,
            reminderNotificationBody = taskWithSubTasks.task.reminderNotificationBody,
        )
    }

    private fun resolveTaskStatus(
        task: Task,
        subTasks: List<SubTask>,
    ): TaskStatus {
        return if (task.completionRule == TaskCompletionRule.AUTO_ALL_SUBTASKS && subTasks.isNotEmpty()) {
            if (subTasks.all { it.isCompleted }) {
                TaskStatus.COMPLETED
            } else {
                TaskStatus.ACTIVE
            }
        } else {
            task.status
        }
    }

    private fun taskComparator(now: Long): Comparator<Task> {
        return compareBy<Task>(
            { taskSortBucket(it, now) },
            { task -> task.dueAt ?: Long.MAX_VALUE },
            { task -> -task.updatedAt },
        )
    }

    private fun taskSortBucket(task: Task, now: Long): Int {
        if (task.status == TaskStatus.COMPLETED) return 5
        if (task.isUrgent || task.priority == TaskPriority.URGENT) return 0

        val dueAt = task.dueAt ?: return 4
        if (dueAt < now) return 1

        val zoneId = timeProvider.zoneId()
        val dueDate = Instant.ofEpochMilli(dueAt).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        return when {
            dueDate == today -> 2
            dueDate == today.plusDays(1) || dueDate == today.plusDays(2) -> 3
            else -> 4
        }
    }

    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            contentMarkdown = contentMarkdown,
            priority = priority.name,
            isUrgent = isUrgent,
            status = status.name,
            startRemindAt = startRemindAt,
            remindWindowEndAt = remindWindowEndAt,
            startReminderMinuteOfDay = startReminderMinuteOfDay,
            windowEndMinuteOfDay = windowEndMinuteOfDay,
            dueAt = dueAt,
            repeatIntervalMinutes = repeatIntervalMinutes,
            exactReminderTimesJson = reminderTimeCodec.encode(exactReminderTimes),
            allDay = allDay,
            completionRule = completionRule.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            tags = tags,
            archived = archived,
            reminderNotificationTitle = reminderNotificationTitle,
            reminderNotificationBody = reminderNotificationBody,
        )
    }

    private fun Task.isOverdue(now: Long): Boolean {
        return status != TaskStatus.COMPLETED &&
            dueAt != null &&
            dueAt < now
    }

    private fun SubTask.toEntity(taskId: String): SubTaskEntity {
        return SubTaskEntity(
            id = id,
            taskId = taskId,
            title = title,
            sortOrder = sortOrder,
            isCompleted = isCompleted,
            completedAt = completedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = false,
        )
    }

    private fun String.toTaskPriority(): TaskPriority {
        return TaskPriority.entries.firstOrNull { it.name == this } ?: TaskPriority.NORMAL
    }

    private fun String.toTaskCompletionRule(): TaskCompletionRule {
        return TaskCompletionRule.entries.firstOrNull { it.name == this }
            ?: TaskCompletionRule.MANUAL
    }

    private fun String.toTaskStatus(): TaskStatus {
        return TaskStatus.entries.firstOrNull { it.name == this } ?: TaskStatus.ACTIVE
    }
}
