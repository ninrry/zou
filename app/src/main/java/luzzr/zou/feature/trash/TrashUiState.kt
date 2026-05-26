package luzzr.zou.feature.trash

import luzzr.zou.domain.model.TrashItemType

data class TrashUiState(
    val title: String = "回收站",
    val items: List<TrashItemCardUiModel> = emptyList(),
    val isEmpty: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val resultMessage: String? = null,
)

data class TrashItemCardUiModel(
    val id: String,
    val type: TrashItemType,
    val title: String,
    val previewText: String,
    val deletedAtText: String,
)
