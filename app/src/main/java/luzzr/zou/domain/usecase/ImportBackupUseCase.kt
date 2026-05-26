package luzzr.zou.domain.usecase

import luzzr.zou.domain.repository.BackupOperationResult
import luzzr.zou.domain.repository.BackupRepository
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(sourceUri: String): BackupOperationResult {
        return backupRepository.importBackup(sourceUri)
    }
}
