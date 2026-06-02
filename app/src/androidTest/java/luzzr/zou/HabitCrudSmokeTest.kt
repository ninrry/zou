package luzzr.zou

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class HabitCrudSmokeTest {

    private val startupPermissionRule = StartupPermissionSetupRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(startupPermissionRule)
        .around(composeRule)

    @Test
    fun opensCreateHabitScreen() {
        val title = "HabitSmoke${System.currentTimeMillis()}"

        assertTagExists("nav_habits", useUnmergedTree = true, timeoutMillis = 30_000)
        composeRule.onNodeWithTag("nav_habits", useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("top_level_habits"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("top_level_create_fab").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("habit_editor_title_input"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("habit_editor_title_input").performTextInput(title)
    }

    private fun assertTagExists(
        tag: String,
        useUnmergedTree: Boolean = false,
        timeoutMillis: Long = 5_000,
    ) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }
}
