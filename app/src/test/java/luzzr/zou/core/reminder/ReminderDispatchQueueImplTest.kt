package luzzr.zou.core.reminder

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderDispatchQueueImplTest {

    @Test
    fun `scheduler exception does not crash dispatch queue`() = runTest {
        val scheduler = object : ReminderScheduler {
            override suspend fun scheduleTask(taskId: String) {
                error("boom")
            }
            override suspend fun cancelTask(taskId: String) = Unit
            override suspend fun scheduleHabit(habitId: String) = Unit
            override suspend fun cancelHabit(habitId: String) = Unit
            override suspend fun rescheduleAllActiveReminders() = Unit
        }
        val queue = ReminderDispatchQueueImpl(
            applicationScope = TestScope(StandardTestDispatcher(testScheduler)),
            reminderScheduler = scheduler,
        )

        queue.scheduleTask("task-1")
        advanceUntilIdle()
    }

    @Test
    fun `same resource keeps only latest operation`() = runTest {
        val recorder = RecorderScheduler()
        val queue = ReminderDispatchQueueImpl(
            applicationScope = TestScope(StandardTestDispatcher(testScheduler)),
            reminderScheduler = recorder,
        )

        queue.scheduleTask("task-1")
        queue.cancelTask("task-1")
        advanceUntilIdle()

        assertEquals(0, recorder.scheduledTaskCalls)
        assertEquals(1, recorder.canceledTaskCalls)
    }

    private class RecorderScheduler : ReminderScheduler {
        var scheduledTaskCalls: Int = 0
        var canceledTaskCalls: Int = 0

        override suspend fun scheduleTask(taskId: String) {
            scheduledTaskCalls += 1
        }

        override suspend fun cancelTask(taskId: String) {
            canceledTaskCalls += 1
        }

        override suspend fun scheduleHabit(habitId: String) = Unit

        override suspend fun cancelHabit(habitId: String) = Unit

        override suspend fun rescheduleAllActiveReminders() = Unit
    }
}
