package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitStep
import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject

class SaveHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
) {
    suspend operator fun invoke(
        habit: Habit,
        steps: List<HabitStep>,
    ) {
        repository.saveHabit(habit = habit, steps = steps)
    }
}
