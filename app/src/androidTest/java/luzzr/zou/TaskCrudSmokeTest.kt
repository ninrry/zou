package luzzr.zou

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class TaskCrudSmokeTest {

    private val startupPermissionRule = StartupPermissionSetupRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(startupPermissionRule)
        .around(composeRule)

    @Test
    fun opensTaskEditorAndReturnsToList() {
        composeRule.onNodeWithTag("nav_tasks").performClick()
        assertTagExists("top_level_tasks")

        composeRule.onNodeWithTag("top_level_create_fab").performClick()

        assertTagExists("task_editor_title_input")
        assertTagExists("task_editor_content_input")
        assertTagExists("task_editor_save")

        composeRule.onNodeWithTag("task_editor_title_input").performTextInput("任务编辑页冒烟测试")
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        assertTagExists("top_level_tasks")
    }

    private fun assertTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
    }
}
