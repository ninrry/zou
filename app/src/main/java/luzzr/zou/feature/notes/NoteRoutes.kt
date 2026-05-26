package luzzr.zou.feature.notes

object NoteRoutes {
    const val listRoute = "notes"
    const val createRoute = "note/create"
    const val noteIdArg = "noteId"
    const val editRoute = "note/edit/{$noteIdArg}"
    const val detailRoute = "note/detail/{$noteIdArg}"

    fun editRoute(noteId: String): String = "note/edit/$noteId"
    fun detailRoute(noteId: String): String = "note/detail/$noteId"
}
