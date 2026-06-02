package luzzr.zou

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class NavigationSmokeTest {

    private val startupPermissionRule = StartupPermissionSetupRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(startupPermissionRule)
        .around(composeRule)

    @Test
    fun switchesAcrossAllTopLevelTabs() {
        composeRule.onNodeWithTag("nav_today", useUnmergedTree = true).performClick()
        assertTagExists("top_level_today")

        composeRule.onNodeWithTag("nav_tasks", useUnmergedTree = true).performClick()
        assertTagExists("top_level_tasks")
        assertTagExists("top_level_create_fab")

        composeRule.onNodeWithTag("nav_habits", useUnmergedTree = true).performClick()
        assertTagExists("top_level_habits")

        composeRule.onNodeWithTag("nav_notes", useUnmergedTree = true).performClick()
        assertTagExists("top_level_notes")

        composeRule.onNodeWithTag("nav_today", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("open_settings").performClick()
        assertTagExists("settings_show_completed_tasks")
        assertTagExists("settings_show_today_habits")
        assertTagExists("settings_show_deleted_habits")

        composeRule.onNodeWithTag("settings_back").performClick()
        assertTagExists("top_level_today")
    }

    @Test
    fun returnsToTodayWithoutBouncingBackToPreviousTab() {
        composeRule.onNodeWithTag("nav_today", useUnmergedTree = true).performClick()
        assertTagExists("top_level_today")

        composeRule.onNodeWithTag("nav_tasks", useUnmergedTree = true).performClick()
        assertTagExists("top_level_tasks")

        composeRule.onNodeWithTag("nav_today", useUnmergedTree = true).performClick()
        assertTagExists("top_level_today")
        composeRule.onNodeWithTag("open_settings").performClick()
        assertTagExists("settings_show_completed_tasks")

        composeRule.onNodeWithTag("settings_back").performClick()
        assertTagExists("top_level_today")
    }

    private fun assertTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
    }
}
