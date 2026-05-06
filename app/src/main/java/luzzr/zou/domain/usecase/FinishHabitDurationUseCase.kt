package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.HabitDurationFinishResult
import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject

class FinishHabitDurationUseCase @Inject constructor(
    private val repository: HabitRepository,
) {
    suspend operator fun invoke(
        habitId: String,
        recordDate: Long,
    ): HabitDurationFinishResult {
        return repository.finishHabitDuration(
            habitId = habitId,
            recordDate = recordDate,
        )
    }
}
