package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskCompletedUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String, completed: Boolean) {
        repository.setTaskCompleted(taskId = taskId, completed = completed)
    }
}
