package luzzr.zou.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.usecase.HardDeleteTrashItemUseCase
import luzzr.zou.domain.usecase.ObserveTrashItemsUseCase
import luzzr.zou.domain.usecase.RestoreTrashItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TrashViewModel @Inject constructor(
    observeTrashItemsUseCase: ObserveTrashItemsUseCase,
    private val restoreTrashItemUseCase: RestoreTrashItemUseCase,
    private val hardDeleteTrashItemUseCase: HardDeleteTrashItemUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val operationState = MutableStateFlow(TrashOperationState(isLoading = true))

    val uiState: StateFlow<TrashUiState> = combine(
        observeTrashItemsUseCase()
            .map { items ->
                operationState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                items.map { item ->
                    TrashItemCardUiModel(
                        id = item.id,
                        type = item.type,
                        title = item.title,
                        previewText = item.previewText,
                        deletedAtText = Instant.ofEpochMilli(item.deletedAt)
                            .atZone(timeProvider.zoneId())
                            .format(formatter),
                    )
                }
            }
            .catch { throwable ->
                operationState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "回收站加载失败，请重试。",
                    )
                }
                emit(emptyList())
            },
        operationState,
    ) { items, state ->
        TrashUiState(
            items = items,
            isEmpty = items.isEmpty(),
            isLoading = state.isLoading,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            resultMessage = state.resultMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrashUiState(isLoading = true),
    )

    fun restore(item: TrashItemCardUiModel) {
        viewModelScope.launch {
            operationState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    resultMessage = null,
                )
            }
            runCatching {
                restoreTrashItemUseCase(item.type, item.id)
            }.onSuccess {
                operationState.update {
                    it.copy(
                        isSaving = false,
                        resultMessage = "已恢复：${item.title}",
                    )
                }
            }.onFailure { throwable ->
                operationState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "恢复失败，请重试。",
                    )
                }
            }
        }
    }

    fun hardDelete(item: TrashItemCardUiModel) {
        viewModelScope.launch {
            operationState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    resultMessage = null,
                )
            }
            runCatching {
                hardDeleteTrashItemUseCase(item.type, item.id)
            }.onSuccess {
                operationState.update {
                    it.copy(
                        isSaving = false,
                        resultMessage = "已彻底删除：${item.title}",
                    )
                }
            }.onFailure { throwable ->
                operationState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "彻底删除失败，请重试。",
                    )
                }
            }
        }
    }
}

private data class TrashOperationState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val resultMessage: String? = null,
)
