package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.HabitStepAdvanceResult
import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject

class AdvanceHabitStepUseCase @Inject constructor(
    private val repository: HabitRepository,
) {
    suspend operator fun invoke(
        habitId: String,
        recordDate: Long,
    ): HabitStepAdvanceResult {
        return repository.advanceHabitStep(
            habitId = habitId,
            recordDate = recordDate,
        )
    }
}
