package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleSubTaskCompletedUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String, subTaskId: String, completed: Boolean) {
        repository.setSubTaskCompleted(
            taskId = taskId,
            subTaskId = subTaskId,
            completed = completed,
        )
    }
}
