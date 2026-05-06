package luzzr.zou.domain.usecase

import luzzr.zou.domain.model.TrashItemType
import javax.inject.Inject

class HardDeleteTrashItemUseCase @Inject constructor(
    private val hardDeleteTaskUseCase: HardDeleteTaskUseCase,
    private val hardDeleteHabitUseCase: HardDeleteHabitUseCase,
    private val hardDeleteNoteUseCase: HardDeleteNoteUseCase,
) {
    suspend operator fun invoke(
        itemType: TrashItemType,
        itemId: String,
    ) {
        when (itemType) {
            TrashItemType.TASK -> hardDeleteTaskUseCase(itemId)
            TrashItemType.HABIT -> hardDeleteHabitUseCase(itemId)
            TrashItemType.NOTE -> hardDeleteNoteUseCase(itemId)
        }
    }
}
