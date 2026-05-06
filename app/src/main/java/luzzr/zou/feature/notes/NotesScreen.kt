package luzzr.zou.feature.notes

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouNoteAccent
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.ModuleFab
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouMetaChip
import luzzr.zou.core.ui.ZouStaggeredReveal
import luzzr.zou.core.ui.noteFlowPressScale
import luzzr.zou.core.ui.rememberPressInteractionSource

@Composable
fun NotesRoute(
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onEditNote: (String) -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    NotesScreen(
        uiState = uiState,
        onCreateNote = onCreateNote,
        onOpenNote = onOpenNote,
        onEditNote = onEditNote,
    )
}

@Composable
fun NotesScreen(
    uiState: NotesUiState,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onEditNote: (String) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ModuleFab(
                accentColor = ZouNoteAccent,
                contentDescription = "新建笔记",
                icon = Icons.Default.Add,
                testTag = "notes_fab",
                onClick = onCreateNote,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (uiState.notes.isEmpty()) {
                item {
                    ZouStaggeredReveal(revealKey = "notes_empty", index = 0) {
                        ZouEmptyStateCard(
                            title = uiState.emptyTitle,
                            description = uiState.emptyDescription,
                            accentColor = ZouNoteAccent,
                        )
                    }
                }
            } else {
                items(uiState.notes, key = { it.id }) { note ->
                    val interactionSource = rememberPressInteractionSource()
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_card_${note.id}")
                            .noteFlowPressScale(interactionSource = interactionSource)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onOpenNote(note.id) },
                                onLongClick = { onEditNote(note.id) },
                            ),
                        accentColor = ZouNoteAccent,
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
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
            item {
                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}
