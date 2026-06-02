package luzzr.zou.feature.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouNoteAccent
import luzzr.zou.core.ui.GlassLevel
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.LayoutTokens
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouMetaChip
import luzzr.zou.core.ui.ZouShimmer
import luzzr.zou.core.ui.ZouStaggeredReveal
import luzzr.zou.core.ui.noteFlowPressScale
import luzzr.zou.core.ui.rememberPressInteractionSource

@Composable
fun NotesRoute(
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onEditNote: (String) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    NotesScreen(
        uiState = uiState,
        viewModel = viewModel,
        onCreateNote = onCreateNote,
        onOpenNote = onOpenNote,
        onEditNote = onEditNote,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    )
}

@Composable
fun NotesScreen(
    uiState: NotesUiState,
    viewModel: NotesViewModel,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onEditNote: (String) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.testTag("notes_screen"),
        containerColor = Color.Transparent,
        topBar = {
            AnimatedVisibility(
                visible = uiState.isSelectMode,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LayoutTokens.ScreenHorizontalPadding, vertical = LayoutTokens.Space12),
                    shape = RoundedCornerShape(16.dp),
                    accentColor = ZouNoteAccent,
                    level = GlassLevel.Strong,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.exitSelectMode() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "取消多选",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "已选择 ${uiState.selectedNoteIds.size} 项",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        TextButton(
                            onClick = {
                                val allIds = uiState.notes.map { it.id }.toSet()
                                if (uiState.selectedNoteIds.size == allIds.size) {
                                    viewModel.exitSelectMode()
                                } else {
                                    viewModel.selectAllNotes(allIds)
                                }
                            }
                        ) {
                            val allIds = uiState.notes.map { it.id }.toSet()
                            Text(
                                text = if (uiState.selectedNoteIds.size == allIds.size) "取消全选" else "全选",
                                color = ZouNoteAccent,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = LayoutTokens.ScreenHorizontalPadding, vertical = LayoutTokens.Space12),
                    verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
                ) {
                    if (uiState.isLoading) {
                        item {
                            ZouShimmer(
                                modifier = Modifier.padding(vertical = LayoutTokens.Space8),
                            )
                        }
                    } else if (uiState.notes.isEmpty()) {
                        item {
                            ZouStaggeredReveal(revealKey = "notes_empty", index = 0) {
                                ZouEmptyStateCard(
                                    title = uiState.emptyTitle,
                                    description = uiState.emptyDescription,
                                    accentColor = ZouNoteAccent,
                                    icon = Icons.Outlined.EditNote,
                                )
                            }
                        }
                    } else {
                        items(uiState.notes, key = { it.id }) { note ->
                            val isSelected = uiState.selectedNoteIds.contains(note.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedVisibility(
                                    visible = uiState.isSelectMode,
                                    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                                    exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                ) {
                                    Box(modifier = Modifier.padding(end = 12.dp)) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { viewModel.toggleNoteSelection(note.id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = ZouNoteAccent,
                                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                            )
                                        )
                                    }
                                }

                                val interactionSource = rememberPressInteractionSource()
                                GlassSurface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("note_card_${note.id}")
                                        .noteFlowPressScale(interactionSource = interactionSource)
                                        .combinedClickable(
                                            interactionSource = interactionSource,
                                            onClick = {
                                                if (uiState.isSelectMode) {
                                                    viewModel.toggleNoteSelection(note.id)
                                                } else {
                                                    onOpenNote(note.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!uiState.isSelectMode) {
                                                    viewModel.enterSelectMode(note.id)
                                                }
                                            },
                                        ),
                                    accentColor = if (isSelected) ZouNoteAccent else null,
                                    level = if (isSelected) GlassLevel.Strong else GlassLevel.Normal,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = LayoutTokens.ScreenHorizontalPadding, vertical = LayoutTokens.Space16),
                                        verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (note.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PushPin,
                                                    contentDescription = "已置顶",
                                                    tint = ZouNoteAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = note.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Text(
                                            text = note.previewText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        ZouMetaChip(
                                            text = "最近编辑：${note.updatedAtText}",
                                            accentColor = ZouNoteAccent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(112.dp))
                    }
                }
            }

            // 底部悬浮毛玻璃胶囊操作栏
            AnimatedVisibility(
                visible = uiState.isSelectMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .zIndex(99f)
            ) {
                val context = LocalContext.current
                val isAllPinned = uiState.notes
                    .filter { uiState.selectedNoteIds.contains(it.id) }
                    .all { it.isPinned }

                GlassSurface(
                    shape = RoundedCornerShape(32.dp),
                    accentColor = ZouNoteAccent,
                    level = GlassLevel.Strong,
                ) {
                    Row(
                        modifier = Modifier
                            .height(64.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BulkActionButton(
                            icon = Icons.Outlined.PushPin,
                            label = if (isAllPinned) "取消置顶" else "置顶",
                            tint = if (isAllPinned) ZouNoteAccent else MaterialTheme.colorScheme.onSurface,
                            onClick = { viewModel.bulkPinNotes(!isAllPinned) }
                        )

                        BulkActionButton(
                            icon = Icons.Outlined.FolderZip,
                            label = "导出",
                            onClick = { viewModel.bulkExportNotes(context) }
                        )

                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        BulkActionButton(
                            icon = Icons.Outlined.DeleteOutline,
                            label = "删除",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteConfirm = true }
                        )

                        if (showDeleteConfirm) {
                            NotesDeleteConfirmDialog(
                                count = uiState.selectedNoteIds.size,
                                onConfirm = {
                                    showDeleteConfirm = false
                                    viewModel.bulkSoftDeleteNotes()
                                },
                                onDismiss = { showDeleteConfirm = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp)
            )
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint.copy(alpha = 0.88f),
        )
    }
}

@Composable
private fun NotesDeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "确认删除",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = "确定要将这 $count 篇笔记移入回收站吗？",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(28.dp),
    )
}
