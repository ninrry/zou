package luzzr.zou.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.NoteFlowNoteAccent
import luzzr.zou.core.markdown.MarkdownRenderer
import luzzr.zou.core.ui.NoteFlowEmptyStateCard
import luzzr.zou.core.ui.NoteFlowMetaChip
import luzzr.zou.core.ui.NoteFlowSectionCard

@Composable
fun NoteDetailRoute(
    onNavigateBack: () -> Unit,
    onEditNote: (String) -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    NoteDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEditNote = onEditNote,
    )
}

@Composable
fun NoteDetailScreen(
    uiState: NoteDetailUiState,
    onNavigateBack: () -> Unit,
    onEditNote: (String) -> Unit,
) {
    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp))
                }
            }

            uiState.emptyMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("返回")
                    }
                    NoteFlowEmptyStateCard(
                        title = "笔记不存在或已删除",
                        description = uiState.emptyMessage,
                        accentColor = NoteFlowNoteAccent,
                        actionLabel = "返回列表",
                        actionTestTag = "note_detail_go_back",
                        onActionClick = onNavigateBack,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("返回")
                    }

                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    NoteFlowMetaChip(
                        text = "最近编辑：${uiState.updatedAtText}",
                        accentColor = NoteFlowNoteAccent,
                    )

                    NoteFlowSectionCard(
                        title = "正文",
                        subtitle = null,
                    ) {
                        if (uiState.contentMarkdown.isBlank()) {
                            Text(
                                text = "暂无正文内容",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            MarkdownRenderer(
                                markdown = uiState.contentMarkdown,
                                mediaLookup = uiState.images.associate { it.mediaId to it.localPath },
                            )
                        }
                    }

                    if (uiState.canEdit) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_detail_edit"),
                            onClick = { uiState.noteId?.let(onEditNote) },
                        ) {
                            Text("编辑笔记")
                        }
                    }
                }
            }
        }
    }
}
