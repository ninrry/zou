package luzzr.zou.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import luzzr.zou.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.noteflowPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "noteflow_preferences",
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    override fun observeReminderPreferences(): Flow<ReminderPreferences> {
        return context.noteflowPreferencesDataStore.data.map(::mapPreferences)
    }

    override suspend fun getReminderPreferences(): ReminderPreferences {
        return observeReminderPreferences().first()
    }

    override suspend fun updateReminderPreferences(
        transform: (ReminderPreferences) -> ReminderPreferences,
    ) {
        context.noteflowPreferencesDataStore.edit { preferences ->
            val updated = transform(mapPreferences(preferences))
            writePreferences(preferences, updated)
        }
    }

    override suspend fun replaceReminderPreferences(preferences: ReminderPreferences) {
        context.noteflowPreferencesDataStore.edit { mutablePreferences ->
            writePreferences(mutablePreferences, preferences)
        }
    }

    private fun mapPreferences(preferences: Preferences): ReminderPreferences {
        return ReminderPreferences(
            defaultTaskRepeatIntervalMinutes = preferences[defaultTaskRepeatIntervalKey] ?: 60,
            defaultHabitRepeatIntervalMinutes = preferences[defaultHabitRepeatIntervalKey] ?: 60,
            showCompletedTasks = preferences[showCompletedTasksKey] ?: false,
            showOnlyTodayHabits = preferences[showOnlyTodayHabitsKey] ?: false,
            showDeletedHabits = preferences[showDeletedHabitsKey] ?: false,
            defaultStartDestination = preferences[defaultStartDestinationKey] ?: "today",
            settingsUpdatedAt = preferences[settingsUpdatedAtKey] ?: 0L,
        )
    }

    private fun writePreferences(
        mutablePreferences: androidx.datastore.preferences.core.MutablePreferences,
        preferences: ReminderPreferences,
    ) {
        mutablePreferences[defaultTaskRepeatIntervalKey] = preferences.defaultTaskRepeatIntervalMinutes
        mutablePreferences[defaultHabitRepeatIntervalKey] = preferences.defaultHabitRepeatIntervalMinutes
        mutablePreferences[showCompletedTasksKey] = preferences.showCompletedTasks
        mutablePreferences[showOnlyTodayHabitsKey] = preferences.showOnlyTodayHabits
        mutablePreferences[showDeletedHabitsKey] = preferences.showDeletedHabits
        mutablePreferences[defaultStartDestinationKey] = preferences.defaultStartDestination
        mutablePreferences[settingsUpdatedAtKey] = preferences.settingsUpdatedAt
    }

    private companion object {
        val defaultTaskRepeatIntervalKey = intPreferencesKey("default_task_repeat_interval_minutes")
        val defaultHabitRepeatIntervalKey = intPreferencesKey("default_habit_repeat_interval_minutes")
        val showCompletedTasksKey = booleanPreferencesKey("show_completed_tasks")
        val showOnlyTodayHabitsKey = booleanPreferencesKey("show_only_today_habits")
        val showDeletedHabitsKey = booleanPreferencesKey("show_deleted_habits")
        val defaultStartDestinationKey = stringPreferencesKey("default_start_destination")
        val settingsUpdatedAtKey = longPreferencesKey("settings_updated_at")
    }
}
