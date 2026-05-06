package luzzr.zou.feature.backup

data class BackupRestoreUiState(
    val title: String = "备份与恢复",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val resultMessage: String? = null,
    val warningMessages: List<String> = emptyList(),
) {
    val compatibilityText: String
        get() = "当前备份版本：v3。导入接受 version = 1、2、3，按 ID + updatedAt 合并。"
}
