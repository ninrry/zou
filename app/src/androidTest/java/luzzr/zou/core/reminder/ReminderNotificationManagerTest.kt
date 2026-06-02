package luzzr.zou.core.reminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderNotificationManagerTest {

    private val manager = ReminderNotificationManager(
        context = ApplicationProvider.getApplicationContext(),
        notificationPermissionChecker = NotificationPermissionChecker(
            context = ApplicationProvider.getApplicationContext(),
        ),
    )

    @Test
    fun customTitleAndBodyFallbackBehavesAsExpected() {
        assertEquals("任务标题", manager.resolveNotificationTitle("任务标题", null))
        assertEquals("自定义标题", manager.resolveNotificationTitle("任务标题", "自定义标题"))

        val defaultBody = manager.resolveNotificationBody(
            triggerAtMillis = 1_763_000_000_000L,
            reason = ReminderTriggerReason.START,
            customBody = null,
        )
        assertTrue(defaultBody.contains("开始提醒时间已到"))
        assertEquals(
            "自定义正文",
            manager.resolveNotificationBody(
                triggerAtMillis = 1_763_000_000_000L,
                reason = ReminderTriggerReason.START,
                customBody = "自定义正文",
            ),
        )
    }

    @Test
    fun alertIntentContainsReminderPayload() {
        val intent = manager.buildReminderAlertIntent(
            notificationId = 42,
            title = "提醒标题",
            body = "提醒正文",
            taskId = "task-1",
            habitId = null,
        )

        assertEquals(42, intent.getIntExtra(ReminderConstants.alertNotificationIdExtra, -1))
        assertEquals("提醒标题", intent.getStringExtra(ReminderConstants.alertTitleExtra))
        assertEquals("提醒正文", intent.getStringExtra(ReminderConstants.alertBodyExtra))
        assertEquals("task-1", intent.getStringExtra(ReminderConstants.taskIdExtra))
    }
}
