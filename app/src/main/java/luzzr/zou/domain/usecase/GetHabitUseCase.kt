package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject

class GetHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
) {
    suspend operator fun invoke(
        habitId: String,
        recordDate: Long,
        includeDeleted: Boolean = false,
    ): Habit? {
        return repository.getHabit(
            habitId = habitId,
            recordDate = recordDate,
            includeDeleted = includeDeleted,
        )
    }
}
