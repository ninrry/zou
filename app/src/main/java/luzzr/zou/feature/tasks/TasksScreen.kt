package luzzr.zou.feature.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.ModuleFab
import luzzr.zou.core.ui.MotionTokens
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
    onDeleteTask: (String) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    TasksScreen(
        uiState = uiState,
        onCreateTask = onCreateTask,
        onOpenTask = onOpenTask,
        onEditTask = onEditTask,
        onTaskCompletionToggle = viewModel::onTaskCompletionToggle,
        onDeleteTask = onDeleteTask,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    )
}

@Composable
fun TasksScreen(
    uiState: TasksUiState,
    onCreateTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onTaskCompletionToggle: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
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
                                icon = Icons.AutoMirrored.Outlined.Assignment,
                            )
                        }
                    }
                } else {
                    items(uiState.tasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        onTaskCompletionToggle(task.id, true)
                                        false // snap back
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        onDeleteTask(task.id)
                                        false // snap back
                                    }
                                    else -> false
                                }
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier
                                .animateItem(),
                            backgroundContent = {
                                when (dismissState.currentValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(Color(0xFF4CAF50).copy(alpha = 0.9f))
                                                .padding(horizontal = 24.dp),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "完成",
                                                modifier = Modifier.size(32.dp),
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(Color(0xFFE53935).copy(alpha = 0.9f))
                                                .padding(horizontal = 24.dp),
                                            contentAlignment = Alignment.CenterEnd,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除",
                                                modifier = Modifier.size(32.dp),
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                        ) {
                            TaskCard(
                                item = task,
                                onClick = { onOpenTask(task.id) },
                                onLongClick = { onEditTask(task.id) },
                                onTaskCompletionToggle = onTaskCompletionToggle,
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(112.dp))
                }
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
    modifier: Modifier = Modifier,
) {
    val interactionSource = rememberPressInteractionSource()
    val hapticFeedback = LocalHapticFeedback.current
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${item.id}")
            .noteFlowPressScale(interactionSource = interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
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
                val isCompleted = item.isCompleted
                val checkAnimationProgress by animateFloatAsState(
                    targetValue = if (isCompleted) 1f else 0f,
                    animationSpec = MotionTokens.SpringBouncy,
                    label = "check_anim_progress",
                )
                val checkContainerSize = 32.dp
                Box(
                    modifier = Modifier
                        .size(checkContainerSize)
                        .testTag("task_completion_toggle")
                        .clickable(
                            interactionSource = rememberPressInteractionSource(),
                            indication = null,
                        ) {
                            if (item.canToggleCompletion) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTaskCompletionToggle(item.id, !isCompleted)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // Outer circle - scales with bounce
                    Icon(
                        imageVector = Icons.Outlined.Circle,
                        contentDescription = if (isCompleted) "已完成" else "未完成",
                        modifier = Modifier
                            .size(checkContainerSize)
                            .graphicsLayer {
                                scaleX = 1f - (checkAnimationProgress * 0.7f)
                                scaleY = 1f - (checkAnimationProgress * 0.7f)
                                alpha = 1f - checkAnimationProgress
                            },
                        tint = if (isCompleted) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    // Check circle - scales in with bounce
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = if (isCompleted) "已完成" else null,
                        modifier = Modifier
                            .size(checkContainerSize)
                            .graphicsLayer {
                                scaleX = checkAnimationProgress * 0.85f + 0.15f
                                scaleY = checkAnimationProgress * 0.85f + 0.15f
                                alpha = checkAnimationProgress
                            },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
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
