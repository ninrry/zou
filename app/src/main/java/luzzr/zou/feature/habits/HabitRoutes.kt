package luzzr.zou.feature.habits

object HabitRoutes {
    const val listRoute = "habits"
    const val createRoute = "habit/create"
    const val habitIdArg = "habitId"
    const val editRoute = "habit/edit/{$habitIdArg}"
    const val detailRoute = "habit/detail/{$habitIdArg}"

    fun editRoute(habitId: String): String = "habit/edit/$habitId"

    fun detailRoute(habitId: String): String = "habit/detail/$habitId"
}
