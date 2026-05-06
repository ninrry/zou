package luzzr.zou.feature.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.NoteFlowTodayAccent
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.NoteFlowEmptyStateCard
import luzzr.zou.core.ui.NoteFlowPageHeader
import luzzr.zou.core.ui.noteFlowOutlinedButtonColors

@Composable
fun TrashRoute(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    TrashScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRestore = viewModel::restore,
        onHardDelete = viewModel::hardDelete,
    )
}

@Composable
fun TrashScreen(
    uiState: TrashUiState,
    onNavigateBack: () -> Unit,
    onRestore: (TrashItemCardUiModel) -> Unit,
    onHardDelete: (TrashItemCardUiModel) -> Unit,
) {
    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        if (uiState.isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("trash_empty_state"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.testTag("trash_back"),
                    onClick = onNavigateBack,
                    enabled = !uiState.isSaving,
                    colors = noteFlowOutlinedButtonColors(),
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    Text("返回", modifier = Modifier.padding(start = 8.dp))
                }
                NoteFlowPageHeader(
                    title = uiState.title,
                    subtitle = "软删除内容会集中显示在这里。",
                )
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.testTag("trash_loading"))
                }
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        modifier = Modifier.testTag("trash_error"),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.resultMessage?.let {
                    Text(
                        text = it,
                        modifier = Modifier.testTag("trash_result"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NoteFlowEmptyStateCard(
                    title = "回收站为空",
                    description = "软删除的任务、习惯和笔记会显示在这里。",
                    accentColor = NoteFlowTodayAccent,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedButton(
                        modifier = Modifier.testTag("trash_back"),
                        onClick = onNavigateBack,
                        enabled = !uiState.isSaving,
                        colors = noteFlowOutlinedButtonColors(),
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        Text("返回", modifier = Modifier.padding(start = 8.dp))
                    }
                    NoteFlowPageHeader(
                        title = uiState.title,
                        subtitle = "可以恢复，也可以彻底删除。",
                    )
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.testTag("trash_loading"))
                    }
                    uiState.errorMessage?.let {
                        Text(
                            text = it,
                            modifier = Modifier.testTag("trash_error"),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    uiState.resultMessage?.let {
                        Text(
                            text = it,
                            modifier = Modifier.testTag("trash_result"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(uiState.items, key = { "${it.type}_${it.id}" }) { item ->
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trash_item_${item.type}_${item.id}"),
                        accentColor = NoteFlowTodayAccent,
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = item.previewText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "删除时间：${item.deletedAtText}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                OutlinedButton(
                                    modifier = Modifier.testTag("trash_restore_${item.id}"),
                                    onClick = { onRestore(item) },
                                    enabled = !uiState.isSaving,
                                    colors = noteFlowOutlinedButtonColors(),
                                ) {
                                    Text("恢复")
                                }
                                TextButton(
                                    modifier = Modifier.testTag("trash_delete_${item.id}"),
                                    onClick = { onHardDelete(item) },
                                    enabled = !uiState.isSaving,
                                ) {
                                    Text(
                                        text = "彻底删除",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
