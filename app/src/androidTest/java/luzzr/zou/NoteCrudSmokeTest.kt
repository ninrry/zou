package luzzr.zou

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class NoteCrudSmokeTest {

    private val startupPermissionRule = StartupPermissionSetupRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(startupPermissionRule)
        .around(composeRule)

    @Test
    fun createsReadsAndEditsNote() {
        val title = "NoteSmoke${System.currentTimeMillis()}"

        assertTagExists("nav_notes", useUnmergedTree = true, timeoutMillis = 30_000)
        composeRule.onNodeWithTag("nav_notes", useUnmergedTree = true).performClick()
        assertTagExists("top_level_notes")

        composeRule.onNodeWithTag("top_level_create_fab").performClick()
        assertTagExists("note_editor_title_input", timeoutMillis = 30_000)
        assertTagExists("note_editor_content_input")
        assertTagExists("note_editor_save")
        composeRule.onNodeWithTag("note_editor_title_input").performTextInput(title)
        composeRule.onNodeWithTag("note_editor_content_input").performTextInput("# 测试标题")
        composeRule.onNodeWithTag("note_editor_save").performClick()

        assertTextExists(title, timeoutMillis = 30_000)

        composeRule.onNodeWithText(title).performClick()
        assertTagExists("note_detail_edit")
        composeRule.onNodeWithTag("note_detail_edit").performClick()
        assertTagExists("note_editor_content_input", timeoutMillis = 30_000)
        assertTagExists("note_editor_save")
        composeRule.onNodeWithTag("note_editor_content_input").performTextInput("\n补充内容")
        composeRule.onNodeWithTag("note_editor_save").performClick()

        assertTextExists(title, timeoutMillis = 30_000)
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

    private fun assertTextExists(
        text: String,
        timeoutMillis: Long = 5_000,
    ) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty())
    }
}
