package luzzr.zou

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class SettingsNavigationSmokeTest {

    private val startupPermissionRule = StartupPermissionSetupRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(startupPermissionRule)
        .around(composeRule)

    @Test
    fun navigatesFromTodayToSettingsTrashAndBackup() {
        composeRule.onNodeWithTag("open_settings").performClick()
        composeRule.onNodeWithTag("settings_content").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("settings_open_trash").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("trash_back").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("trash_back").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_open_backup").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_content").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("settings_open_backup").performClick()
        assertTagExists("backup_export")
    }

    private fun assertTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
    }
}

