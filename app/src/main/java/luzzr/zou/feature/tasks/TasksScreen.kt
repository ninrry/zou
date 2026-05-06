package luzzr.zou.feature.tasks

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
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
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.ModuleFab
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouMetaChip
import luzzr.zou.core.ui.ZouStaggeredReveal
import luzzr.zou.core.ui.noteFlowPressScale
import luzzr.zou.core.ui.rememberPressInteractionSource

@Composable
fun TasksRoute(
    onCreateTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    TasksScreen(
        uiState = uiState,
        onCreateTask = onCreateTask,
        onOpenTask = onOpenTask,
        onEditTask = onEditTask,
        onTaskCompletionToggle = viewModel::onTaskCompletionToggle,
    )
}

@Composable
fun TasksScreen(
    uiState: TasksUiState,
    onCreateTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onTaskCompletionToggle: (String, Boolean) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ModuleFab(
                accentColor = ZouTaskAccent,
                contentDescription = "新建任务",
                icon = Icons.Default.Add,
                testTag = "tasks_fab",
                onClick = onCreateTask,
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
            if (uiState.tasks.isEmpty()) {
                item {
                    ZouStaggeredReveal(revealKey = "tasks_empty", index = 0) {
                        ZouEmptyStateCard(
                            title = uiState.emptyTitle,
                            description = uiState.emptyDescription,
                            accentColor = ZouTaskAccent,
                        )
                    }
                }
            } else {
                items(uiState.tasks, key = { it.id }) { task ->
                    TaskCard(
                        item = task,
                        onClick = { onOpenTask(task.id) },
                        onLongClick = { onEditTask(task.id) },
                        onTaskCompletionToggle = onTaskCompletionToggle,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun TaskCard(
    item: TaskListItemUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTaskCompletionToggle: (String, Boolean) -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${item.id}")
            .noteFlowPressScale(interactionSource = interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        accentColor = ZouTaskAccent,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZouMetaChip(text = item.priorityLabel)
                        if (item.showUrgentBadge) {
                            ZouMetaChip(
                                text = "紧急",
                                accentColor = ZouTaskAccent,
                            )
                        }
                    }
                }
                Checkbox(
                    modifier = Modifier.testTag("task_completion_toggle"),
                    checked = item.isCompleted,
                    onCheckedChange = if (item.canToggleCompletion) {
                        { checked -> onTaskCompletionToggle(item.id, checked) }
                    } else {
                        null
                    },
                )
            }
            Text(
                text = item.dueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.progressText.isNotBlank()) {
                ZouMetaChip(text = item.progressText, accentColor = ZouTaskAccent)
            }
        }
    }
}
