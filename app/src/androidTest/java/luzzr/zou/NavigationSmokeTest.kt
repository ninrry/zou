package luzzr.zou

import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        assertTagExists("today_summary_card")

        composeRule.onNodeWithTag("nav_tasks").performClick()
        assertTagExists("tasks_fab")

        composeRule.onNodeWithTag("nav_habits").performClick()
        assertTagExists("habits_fab")

        composeRule.onNodeWithTag("nav_notes").performClick()
        assertTagExists("notes_fab")

        composeRule.onNodeWithTag("nav_today").performClick()
        composeRule.onNodeWithTag("open_settings").performClick()
        assertTagExists("settings_show_completed_tasks")
        assertTagExists("settings_show_today_habits")
        assertTagExists("settings_show_deleted_habits")

        composeRule.onNodeWithTag("settings_back").performClick()
        assertTagExists("today_summary_card")
    }

    @Test
    fun returnsToTodayWithoutBouncingBackToPreviousTab() {
        assertTagExists("today_summary_card")

        composeRule.onNodeWithTag("nav_tasks").performClick()
        assertTagExists("tasks_fab")

        composeRule.onNodeWithTag("nav_today").performClick()
        composeRule.onNodeWithTag("open_settings").performClick()
        assertTagExists("settings_show_completed_tasks")

        composeRule.onNodeWithTag("settings_back").performClick()
        assertTagExists("today_summary_card")
    }

    private fun assertTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
    }
}
