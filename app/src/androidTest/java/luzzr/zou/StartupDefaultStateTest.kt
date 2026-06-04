package luzzr.zou

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartupDefaultStateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rendersTopLevelCanvasBeforeStoredPreferencesArrive() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("top_level_canvas").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithTag("top_level_canvas").fetchSemanticsNodes().isNotEmpty())
    }
}
