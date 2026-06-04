package luzzr.zou.feature.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.LocalZouMotion
import luzzr.zou.core.ui.ModuleFab
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouMetaChip
import luzzr.zou.core.ui.ZouStaggeredReveal
import luzzr.zou.core.ui.noteFlowPressScale
import luzzr.zou.core.ui.rememberPressInteractionSource
import luzzr.zou.core.ui.LayoutTokens
import luzzr.zou.core.ui.ZouShimmer
import luzzr.zou.core.designsystem.theme.ZouDesignTokens
import androidx.compose.material3.ripple


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
    val motion = LocalZouMotion.current
    var removingIds by remember { mutableStateOf(emptySet<String>()) }
    Scaffold(
        modifier = Modifier.testTag("tasks_screen"),
        containerColor = Color.Transparent,
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
                    .padding(horizontal = LayoutTokens.ScreenHorizontalPadding, vertical = LayoutTokens.Space12),
                verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
            ) {
                if (uiState.isLoading) {
                    item {
                        ZouShimmer(
                            modifier = Modifier.padding(vertical = LayoutTokens.Space8),
                        )
                    }
                } else if (uiState.tasks.isEmpty()) {
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
                        val dismissState = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismissState.currentValue) {
                            when (dismissState.currentValue) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    onTaskCompletionToggle(task.id, true)
                                    dismissState.reset()
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    removingIds = removingIds + task.id
                                    dismissState.reset()
                                    delay(motion.listExitDelayMillis.toLong())
                                    onDeleteTask(task.id)
                                }
                                else -> Unit
                            }
                        }
                        AnimatedVisibility(
                            visible = task.id !in removingIds,
                            exit = fadeOut(animationSpec = motion.listExit) +
                                shrinkVertically(animationSpec = motion.listExitSize),
                        ) {
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
                                                .background(ZouDesignTokens.colors.success.copy(alpha = 0.9f))
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
                                                .background(ZouDesignTokens.colors.danger.copy(alpha = 0.9f))
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
    val motion = LocalZouMotion.current
    val hapticFeedback = LocalHapticFeedback.current
    // 动态色彩流转：当任务被勾选为“已完成”时，色彩平滑流变流转为烟灰色，未完成则呈饱满的任务亮色
    val currentAccentColor = if (item.isCompleted) {
        ZouDesignTokens.colors.textTertiary
    } else {
        ZouTaskAccent
    }
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
        accentColor = currentAccentColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LayoutTokens.ScreenHorizontalPadding, vertical = LayoutTokens.Space16),
            verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
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
                    animationSpec = motion.press,
                    label = "check_anim_progress",
                )
                val checkIconSize = 32.dp
                val touchTargetSize = 48.dp
                Box(
                    modifier = Modifier
                        .size(touchTargetSize) // 物理热区加宽至标准的 48.dp，极其方便点击
                        .testTag("task_completion_toggle")
                        .clickable(
                            interactionSource = rememberPressInteractionSource(),
                            indication = ripple(
                                bounded = false, // 启用无边界 Ripple，让水波纹温润漾开并溢出到卡片上
                                radius = 24.dp
                            ),
                            onClickLabel = if (isCompleted) "取消完成" else "标记完成",
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
                            .size(checkIconSize)
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
                            .size(checkIconSize)
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
