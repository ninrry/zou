package luzzr.zou.feature.tasks

object TaskRoutes {
    const val listRoute = "tasks"
    const val createRoute = "task/create"
    const val taskIdArg = "taskId"
    const val editRoute = "task/edit/{$taskIdArg}"
    const val detailRoute = "task/detail/{$taskIdArg}"

    fun editRoute(taskId: String): String = "task/edit/$taskId"

    fun detailRoute(taskId: String): String = "task/detail/$taskId"
}
