package luzzr.zou.domain.usecase

import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.repository.SettingsRepository
import javax.inject.Inject

class GetReminderPreferencesUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): ReminderPreferences = settingsRepository.getReminderPreferences()
}
