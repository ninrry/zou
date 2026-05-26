package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.TrashItemType
import javax.inject.Inject

class RestoreTrashItemUseCase @Inject constructor(
    private val restoreTaskUseCase: RestoreTaskUseCase,
    private val restoreHabitUseCase: RestoreHabitUseCase,
    private val restoreNoteUseCase: RestoreNoteUseCase,
) {
    suspend operator fun invoke(
        itemType: TrashItemType,
        itemId: String,
    ) {
        when (itemType) {
            TrashItemType.TASK -> restoreTaskUseCase(itemId)
            TrashItemType.HABIT -> restoreHabitUseCase(itemId)
            TrashItemType.NOTE -> restoreNoteUseCase(itemId)
        }
    }
}
