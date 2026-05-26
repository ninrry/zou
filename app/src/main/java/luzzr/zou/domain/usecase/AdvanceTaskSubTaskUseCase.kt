package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.TaskSubTaskAdvanceResult
import luzzr.zou.domain.repository.TaskRepository
import javax.inject.Inject

class AdvanceTaskSubTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): TaskSubTaskAdvanceResult {
        return repository.advanceTaskSubTask(taskId)
    }
}
