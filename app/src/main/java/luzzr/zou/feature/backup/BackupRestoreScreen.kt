package luzzr.zou.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.NoteFlowTodayAccent
import luzzr.zou.core.ui.NoteFlowPageHeader
import luzzr.zou.core.ui.NoteFlowSectionCard
import luzzr.zou.core.ui.noteFlowButtonColors
import luzzr.zou.core.ui.noteFlowOutlinedButtonColors

@Composable
fun BackupRestoreRoute(
    onNavigateBack: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let { viewModel.exportBackup(it.toString()) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importBackup(it.toString()) }
    }
    BackupRestoreScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onExportBackup = { exportLauncher.launch("jielv-backup-v3.zip") },
        onImportBackup = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
    )
}

@Composable
fun BackupRestoreScreen(
    uiState: BackupRestoreUiState,
    onNavigateBack: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
) {
    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.testTag("backup_back"),
                onClick = onNavigateBack,
                enabled = !uiState.isSaving,
                colors = noteFlowOutlinedButtonColors(),
            ) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Text("返回", modifier = Modifier.padding(start = 8.dp))
            }
            NoteFlowPageHeader(
                title = uiState.title,
                subtitle = "导出和导入本地备份文件。",
            )

            NoteFlowSectionCard(
                title = "备份兼容策略",
                accentColor = NoteFlowTodayAccent,
            ) {
                Text(
                    text = uiState.compatibilityText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_export"),
                    enabled = !uiState.isSaving,
                    onClick = onExportBackup,
                    colors = noteFlowButtonColors(NoteFlowTodayAccent),
                ) {
                    Text(if (uiState.isSaving) "处理中" else "导出备份 zip")
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_import"),
                    enabled = !uiState.isSaving,
                    onClick = onImportBackup,
                    colors = noteFlowButtonColors(NoteFlowTodayAccent),
                ) {
                    Text(if (uiState.isSaving) "处理中" else "导入备份 zip")
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.testTag("backup_loading"))
            }
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier.testTag("backup_error"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            uiState.resultMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier.testTag("backup_result"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (uiState.warningMessages.isNotEmpty()) {
                Text(
                    text = "警告",
                    modifier = Modifier.testTag("backup_warning_title"),
                    style = MaterialTheme.typography.titleMedium,
                    color = NoteFlowTodayAccent,
                )
                uiState.warningMessages.forEachIndexed { index, warning ->
                    Text(
                        text = warning,
                        modifier = Modifier.testTag("backup_warning_$index"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
