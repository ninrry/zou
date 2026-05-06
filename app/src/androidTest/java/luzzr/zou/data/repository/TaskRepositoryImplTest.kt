package luzzr.zou.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import luzzr.zou.core.reminder.ReminderDispatchQueue
import luzzr.zou.core.reminder.ReminderTimeCodec
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.NoteFlowDatabase
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import luzzr.zou.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskRepositoryImplTest {

    private lateinit var database: NoteFlowDatabase
    private lateinit var repository: TaskRepositoryImpl
    private lateinit var reminderDispatchQueue: FakeReminderDispatchQueue
    private val reminderTimeCodec = ReminderTimeCodec()
    private val timeProvider = FixedTimeProvider()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            NoteFlowDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        reminderDispatchQueue = FakeReminderDispatchQueue()
        repository = TaskRepositoryImpl(
            taskDao = database.taskDao(),
            reminderDispatchQueue = reminderDispatchQueue,
            reminderTimeCodec = reminderTimeCodec,
            timeProvider = timeProvider,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sortsTasksByDefaultRule() = runBlocking {
        repository.saveTask(
            task = task(title = "其他任务", dueAt = dueAt(daysOffset = 5)),
            subTasks = emptyList(),
        )
        repository.saveTask(
            task = task(title = "明天到期", dueAt = dueAt(daysOffset = 1)),
            subTasks = emptyList(),
        )
        repository.saveTask(
            task = task(title = "今天到期", dueAt = dueAt(daysOffset = 0)),
            subTasks = emptyList(),
        )
        repository.saveTask(
            task = task(title = "已逾期", dueAt = dueAt(daysOffset = -1)),
            subTasks = emptyList(),
        )
        repository.saveTask(
            task = task(title = "紧急任务", isUrgent = true),
            subTasks = emptyList(),
        )

        val titles = repository.observeTasks(includeCompleted = false)
            .first()
            .map { it.title }

        assertEquals(
            listOf("紧急任务", "今天到期", "明天到期", "其他任务"),
            titles,
        )
    }

    @Test
    fun hidesOverdueActiveTasksFromObservedLists() = runBlocking {
        repository.saveTask(
            task = task(title = "已逾期", dueAt = dueAt(daysOffset = -1)),
            subTasks = emptyList(),
        )

        val tasks = repository.observeTasks(includeCompleted = true).first()

        assertTrue(tasks.none { it.title == "已逾期" })
    }

    @Test
    fun saveTaskSchedulesReminderEvaluation() = runBlocking {
        val taskId = UUID.randomUUID().toString()

        repository.saveTask(
            task = task(
                id = taskId,
                title = "需要提醒的任务",
                startRemindAt = timeProvider.nowMillis() + 15 * 60_000L,
            ),
            subTasks = emptyList(),
        )

        assertTrue(reminderDispatchQueue.scheduledTaskIds.contains(taskId))
    }

    @Test
    fun softDeleteRemovesTaskAndSoftDeletesSubTasks() = runBlocking {
        val taskId = UUID.randomUUID().toString()
        repository.saveTask(
            task = task(
                id = taskId,
                title = "待删除任务",
            ),
            subTasks = listOf(
                SubTask(
                    id = UUID.randomUUID().toString(),
                    title = "子任务一",
                ),
            ),
        )

        repository.softDeleteTask(taskId)

        assertTrue(repository.observeTasks(includeCompleted = true).first().isEmpty())
        val rawGraph = database.taskDao().getTaskWithSubTasks(taskId)
        assertTrue(rawGraph?.task?.isDeleted == true)
        assertTrue(rawGraph?.subTasks?.all { it.isDeleted } == true)
        assertTrue(reminderDispatchQueue.canceledTaskIds.contains(taskId))
    }

    @Test
    fun restoreTaskBringsBackTaskAndSubTasks() = runBlocking {
        val taskId = UUID.randomUUID().toString()
        val subTaskId = UUID.randomUUID().toString()
        repository.saveTask(
            task = task(id = taskId, title = "Restorable task"),
            subTasks = listOf(
                SubTask(
                    id = subTaskId,
                    title = "Child",
                ),
            ),
        )
        repository.softDeleteTask(taskId)

        repository.restoreTask(taskId)

        val restored = repository.observeTasks(includeCompleted = false).first().single()
        assertEquals(taskId, restored.id)
        assertEquals(listOf(subTaskId), restored.subTasks.map { it.id })
        assertTrue(reminderDispatchQueue.scheduledTaskIds.contains(taskId))
    }

    @Test
    fun hardDeleteRemovesTaskTreePermanently() = runBlocking {
        val taskId = UUID.randomUUID().toString()
        repository.saveTask(
            task = task(id = taskId, title = "Delete forever"),
            subTasks = listOf(
                SubTask(
                    id = UUID.randomUUID().toString(),
                    title = "Child",
                ),
            ),
        )

        repository.hardDeleteTask(taskId)

        assertTrue(database.taskDao().getTaskWithSubTasks(taskId) == null)
        assertTrue(reminderDispatchQueue.canceledTaskIds.contains(taskId))
    }

    @Test
    fun autoCompletionRuleFollowsSubTasksAndReschedulesReminder() = runBlocking {
        val taskId = UUID.randomUUID().toString()
        val subTaskOne = SubTask(
            id = UUID.randomUUID().toString(),
            title = "步骤一",
            isCompleted = false,
        )
        val subTaskTwo = SubTask(
            id = UUID.randomUUID().toString(),
            title = "步骤二",
            isCompleted = true,
        )

        repository.saveTask(
            task = task(
                id = taskId,
                title = "自动完成任务",
                completionRule = TaskCompletionRule.AUTO_ALL_SUBTASKS,
                startRemindAt = timeProvider.nowMillis() + 15 * 60_000L,
            ),
            subTasks = listOf(subTaskOne, subTaskTwo),
        )

        assertEquals(TaskStatus.ACTIVE, repository.getTask(taskId)?.status)

        repository.setSubTaskCompleted(taskId, subTaskOne.id, true)
        assertEquals(TaskStatus.COMPLETED, repository.getTask(taskId)?.status)

        repository.setSubTaskCompleted(taskId, subTaskTwo.id, false)
        assertEquals(TaskStatus.ACTIVE, repository.getTask(taskId)?.status)
        assertTrue(reminderDispatchQueue.scheduledTaskIds.count { it == taskId } >= 3)
    }

    private fun task(
        id: String = UUID.randomUUID().toString(),
        title: String,
        dueAt: Long? = null,
        startRemindAt: Long? = null,
        isUrgent: Boolean = false,
        completionRule: TaskCompletionRule = TaskCompletionRule.MANUAL,
    ): Task {
        return Task(
            id = id,
            title = title,
            priority = TaskPriority.NORMAL,
            isUrgent = isUrgent,
            status = TaskStatus.ACTIVE,
            startRemindAt = startRemindAt,
            dueAt = dueAt,
            completionRule = completionRule,
        )
    }

    private fun dueAt(daysOffset: Long): Long {
        return LocalDateTime.of(
            LocalDate.of(2026, 3, 8).plusDays(daysOffset),
            LocalTime.of(18, 0),
        )
            .atZone(timeProvider.zoneId())
            .toInstant()
            .toEpochMilli()
    }

    private class FixedTimeProvider : TimeProvider {
        private val zoneId = ZoneId.of("Asia/Singapore")

        override fun nowMillis(): Long {
            return LocalDateTime.of(2026, 3, 8, 9, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }

        override fun zoneId(): ZoneId = zoneId
    }

    private class FakeReminderDispatchQueue : ReminderDispatchQueue {
        val scheduledTaskIds = mutableListOf<String>()
        val canceledTaskIds = mutableListOf<String>()

        override fun scheduleTask(taskId: String) {
            scheduledTaskIds += taskId
        }

        override fun cancelTask(taskId: String) {
            canceledTaskIds += taskId
        }

        override fun scheduleHabit(habitId: String) = Unit

        override fun cancelHabit(habitId: String) = Unit

        override fun rescheduleAllActiveReminders() = Unit
    }
}
