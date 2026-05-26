package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.HabitStepAdvanceResult
import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject

class RevertHabitStepUseCase @Inject constructor(
    private val repository: HabitRepository,
) {
    suspend operator fun invoke(
        habitId: String,
        recordDate: Long,
        stepId: String,
    ): HabitStepAdvanceResult {
        return repository.revertHabitStep(
            habitId = habitId,
            recordDate = recordDate,
            stepId = stepId,
        )
    }
}
