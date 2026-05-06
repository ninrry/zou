package luzzr.zou.domain.repository

data class BackupOperationResult(
    val success: Boolean,
    val message: String,
    val warnings: List<String> = emptyList(),
)

interface BackupRepository {
    suspend fun exportBackup(destinationUri: String): BackupOperationResult

    suspend fun importBackup(sourceUri: String): BackupOperationResult
}
