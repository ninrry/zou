package luzzr.zou.domain.model

data class TaskSubTaskAdvanceResult(
    val progressedSubTaskId: String? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val taskCompleted: Boolean = false,
)

data class HabitStepAdvanceResult(
    val progressedStepId: String? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val habitCompleted: Boolean = false,
)

data class HabitDurationFinishResult(
    val completed: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val targetMinutes: Int? = null,
)
