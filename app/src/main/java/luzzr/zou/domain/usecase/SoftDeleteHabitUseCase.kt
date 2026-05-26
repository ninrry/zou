package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject

class SoftDeleteHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
) {
    suspend operator fun invoke(habitId: String) {
        repository.softDeleteHabit(habitId)
    }
}
