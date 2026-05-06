package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.Task
import luzzr.zou.domain.repository.TaskRepository
import javax.inject.Inject

class SaveTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(task: Task, subTasks: List<SubTask>) {
        repository.saveTask(task = task, subTasks = subTasks)
    }
}
