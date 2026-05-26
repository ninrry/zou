package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.BackupOperationResult
import luzzr.zou.domain.repository.BackupRepository
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(destinationUri: String): BackupOperationResult {
        return backupRepository.exportBackup(destinationUri)
    }
}
