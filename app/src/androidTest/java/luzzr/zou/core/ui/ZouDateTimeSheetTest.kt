package luzzr.zou.core.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.designsystem.theme.ZouTheme
import java.time.LocalDateTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZouDateTimeSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quickDateAndConfirmActionsAreClickable() {
        composeRule.setContent {
            ZouTheme {
                ZouDateTimeSheet(
                    visible = true,
                    title = "Select",
                    mode = ZouDateTimeSheetMode.DATE_TIME,
                    initialDateTime = LocalDateTime.of(2026, 3, 11, 9, 7),
                    accentColor = ZouTaskAccent,
                    onDismissRequest = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        assertTagExists("datetime_quick_tomorrow")
        composeRule.onNodeWithTag("datetime_quick_tomorrow").performClick()
        assertTagExists("datetime_confirm")
        composeRule.onNodeWithTag("datetime_confirm").performClick()
        assertTagExists("datetime_next")
        composeRule.onNodeWithTag("datetime_next").performClick()
        composeRule.onNodeWithTag("datetime_confirm").performClick()
        assertTagExists("datetime_sheet_preview")
    }

    @Test
    fun precisionToggleAndCancelActionAreVisible() {
        composeRule.setContent {
            ZouTheme {
                ZouDateTimeSheet(
                    visible = true,
                    title = "Select",
                    mode = ZouDateTimeSheetMode.TIME_ONLY,
                    initialDateTime = LocalDateTime.of(2026, 3, 11, 9, 7),
                    accentColor = ZouTaskAccent,
                    onDismissRequest = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        assertTagExists("datetime_precision_one")
        composeRule.onNodeWithTag("datetime_precision_one").performClick()
        assertTagExists("datetime_cancel")
        assertTagExists("datetime_confirm")
    }

    private fun assertTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
    }
}
