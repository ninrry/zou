package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.TaskRepository
import javax.inject.Inject

class SoftDeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String) {
        repository.softDeleteTask(taskId)
    }
}
