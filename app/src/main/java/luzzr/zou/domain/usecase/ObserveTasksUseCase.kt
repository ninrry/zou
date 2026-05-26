package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.Task
import luzzr.zou.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(includeCompleted: Boolean): Flow<List<Task>> {
        return repository.observeTasks(includeCompleted = includeCompleted)
    }
}
