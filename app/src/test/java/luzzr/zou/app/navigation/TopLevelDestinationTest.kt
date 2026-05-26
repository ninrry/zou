package luzzr.zou.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun exposesFourRootDestinations() {
        assertEquals(
            listOf("today", "tasks", "habits", "notes"),
            TopLevelDestination.entries.map { it.route },
        )
    }
}
