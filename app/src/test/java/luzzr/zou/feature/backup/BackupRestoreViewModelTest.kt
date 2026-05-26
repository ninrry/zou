package luzzr.zou.feature.backup

import luzzr.zou.MainDispatcherRule
import luzzr.zou.domain.repository.BackupOperationResult
import luzzr.zou.domain.repository.BackupRepository
import luzzr.zou.domain.usecase.ExportBackupUseCase
import luzzr.zou.domain.usecase.ImportBackupUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun mapsSuccessfulImportIntoResultAndWarnings() = runTest {
        val repository = FakeBackupRepository(
            importResult = BackupOperationResult(
                success = true,
                message = "导入成功",
                warnings = listOf("media missing"),
            ),
        )
        val viewModel = BackupRestoreViewModel(
            exportBackupUseCase = ExportBackupUseCase(repository),
            importBackupUseCase = ImportBackupUseCase(repository),
        )

        viewModel.importBackup("file://backup.zip")
        advanceUntilIdle()

        assertEquals("导入成功", viewModel.uiState.value.resultMessage)
        assertEquals(listOf("media missing"), viewModel.uiState.value.warningMessages)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun mapsFailedExportIntoErrorMessage() = runTest {
        val repository = FakeBackupRepository(
            exportResult = BackupOperationResult(
                success = false,
                message = "导出失败",
            ),
        )
        val viewModel = BackupRestoreViewModel(
            exportBackupUseCase = ExportBackupUseCase(repository),
            importBackupUseCase = ImportBackupUseCase(repository),
        )

        viewModel.exportBackup("file://backup.zip")
        advanceUntilIdle()

        assertEquals("导出失败", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.resultMessage)
    }

    private class FakeBackupRepository(
        private val exportResult: BackupOperationResult = BackupOperationResult(true, "ok"),
        private val importResult: BackupOperationResult = BackupOperationResult(true, "ok"),
    ) : BackupRepository {
        override suspend fun exportBackup(destinationUri: String): BackupOperationResult = exportResult

        override suspend fun importBackup(sourceUri: String): BackupOperationResult = importResult
    }
}
