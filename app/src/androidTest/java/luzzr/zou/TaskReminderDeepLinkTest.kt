package luzzr.zou

import android.content.Intent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import luzzr.zou.core.reminder.ReminderConstants
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class TaskReminderDeepLinkTest {

    private val startupPermissionRule = StartupPermissionSetupRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(startupPermissionRule)
        .around(composeRule)

    @Test
    fun notificationIntentNavigatesToTaskDetailScreen() {
        composeRule.runOnUiThread {
            composeRule.activity.handleReminderIntent(
                Intent().putExtra(ReminderConstants.taskIdExtra, "missing-task"),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("任务不存在或已删除").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("任务不存在或已删除").assertTextEquals("任务不存在或已删除")
        composeRule.onNodeWithText("返回列表").assertTextEquals("返回列表")
    }
}
