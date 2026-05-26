package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject

class HardDeleteHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
) {
    suspend operator fun invoke(habitId: String) {
        habitRepository.hardDeleteHabit(habitId)
    }
}
