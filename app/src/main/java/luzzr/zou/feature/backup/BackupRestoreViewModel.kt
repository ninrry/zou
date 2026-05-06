package luzzr.zou.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.zou.domain.repository.BackupOperationResult
import luzzr.zou.domain.usecase.ExportBackupUseCase
import luzzr.zou.domain.usecase.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState = _uiState.asStateFlow()

    fun exportBackup(destinationUri: String) {
        runOperation {
            exportBackupUseCase(destinationUri)
        }
    }

    fun importBackup(sourceUri: String) {
        runOperation {
            importBackupUseCase(sourceUri)
        }
    }

    private fun runOperation(block: suspend () -> BackupOperationResult) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isSaving = true,
                    errorMessage = null,
                    resultMessage = null,
                    warningMessages = emptyList(),
                )
            }
            val result = runCatching {
                block()
            }.getOrElse { throwable ->
                BackupOperationResult(
                    success = false,
                    message = throwable.message ?: "备份操作失败，请重试。",
                )
            }
            _uiState.update {
                if (result.success) {
                    it.copy(
                        isLoading = false,
                        isSaving = false,
                        errorMessage = null,
                        resultMessage = result.message,
                        warningMessages = result.warnings,
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        isSaving = false,
                        errorMessage = result.message,
                        resultMessage = null,
                        warningMessages = result.warnings,
                    )
                }
            }
        }
    }
}
