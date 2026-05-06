package luzzr.zou.domain.usecase

import luzzr.zou.core.reminder.ReminderDispatchQueue
import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateReminderPreferencesUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderDispatchQueue: ReminderDispatchQueue,
) {
    suspend operator fun invoke(
        transform: (ReminderPreferences) -> ReminderPreferences,
    ) {
        settingsRepository.updateReminderPreferences(transform)
        reminderDispatchQueue.rescheduleAllActiveReminders()
    }
}
