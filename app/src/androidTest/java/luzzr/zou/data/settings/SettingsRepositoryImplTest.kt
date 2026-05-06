package luzzr.zou.data.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryImplTest {

    @Test
    fun persistsReminderPreferencesAcrossReads() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepositoryImpl(context)
        val baseline = ReminderPreferences()
        repository.replaceReminderPreferences(baseline)

        repository.updateReminderPreferences {
            it.copy(
                defaultTaskRepeatIntervalMinutes = 45,
                defaultHabitRepeatIntervalMinutes = 90,
                showCompletedTasks = true,
                showOnlyTodayHabits = true,
                showDeletedHabits = true,
                settingsUpdatedAt = 1234L,
            )
        }

        val restored = repository.getReminderPreferences()

        assertEquals(45, restored.defaultTaskRepeatIntervalMinutes)
        assertEquals(90, restored.defaultHabitRepeatIntervalMinutes)
        assertEquals(true, restored.showCompletedTasks)
        assertEquals(true, restored.showOnlyTodayHabits)
        assertEquals(true, restored.showDeletedHabits)
        assertEquals(1234L, restored.settingsUpdatedAt)

        repository.replaceReminderPreferences(baseline)
    }
}
