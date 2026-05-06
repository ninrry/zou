package luzzr.zou.data.local.database.model

import androidx.room.Embedded
import androidx.room.Relation
import luzzr.zou.data.local.database.entity.SubTaskEntity
import luzzr.zou.data.local.database.entity.TaskEntity

data class TaskWithSubTasks(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId",
    )
    val subTasks: List<SubTaskEntity>,
)
