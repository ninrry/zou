package luzzr.zou.domain.usecase

import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveReminderPreferencesUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<ReminderPreferences> = settingsRepository.observeReminderPreferences()
}
