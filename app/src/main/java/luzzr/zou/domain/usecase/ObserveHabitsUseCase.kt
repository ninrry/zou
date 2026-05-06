package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.repository.HabitRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveHabitsUseCase @Inject constructor(
    private val repository: HabitRepository,
) {
    operator fun invoke(
        recordDate: Long,
        includeDeleted: Boolean,
    ): Flow<List<Habit>> {
        return repository.observeHabits(
            recordDate = recordDate,
            includeDeleted = includeDeleted,
        )
    }
}
