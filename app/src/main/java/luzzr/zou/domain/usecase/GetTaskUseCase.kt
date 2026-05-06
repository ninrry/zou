package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.Task
import luzzr.zou.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Task? = repository.getTask(taskId)
}
