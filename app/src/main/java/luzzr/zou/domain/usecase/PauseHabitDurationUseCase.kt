package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject

class PauseHabitDurationUseCase @Inject constructor(
    private val repository: HabitRepository,
) {
    suspend operator fun invoke(
        habitId: String,
        recordDate: Long,
    ) {
        repository.pauseHabitDuration(
            habitId = habitId,
            recordDate = recordDate,
        )
    }
}
