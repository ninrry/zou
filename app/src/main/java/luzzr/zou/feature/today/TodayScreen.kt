package luzzr.zou.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.NoteFlowHabitAccent
import luzzr.zou.core.designsystem.theme.NoteFlowTaskAccent
import luzzr.zou.core.ui.NoteFlowStaggeredReveal
import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.TaskQuickActionType
import kotlinx.coroutines.flow.Flow

@Composable
fun TodayRoute(
    onCreateTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onOpenTasks: () -> Unit,
    onCreateHabit: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    TodayScreen(
        uiState = uiState,
        onCreateTask = onCreateTask,
        onOpenTask = onOpenTask,
        onEditTask = onEditTask,
        onOpenTasks = onOpenTasks,
        onTaskAction = viewModel::onTaskAction,
        onCreateHabit = onCreateHabit,
        onOpenHabit = onOpenHabit,
        onEditHabit = onEditHabit,
        onOpenHabits = onOpenHabits,
        onHabitPrimaryAction = viewModel::onHabitPrimaryAction,
        onHabitSecondaryAction = viewModel::onHabitSecondaryAction,
        events = viewModel.events,
        onUndo = viewModel::onUndo,
        onUndoExpired = viewModel::onUndoExpired,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onCreateTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onOpenTasks: () -> Unit,
    onTaskAction: (String, TaskQuickActionType) -> Unit,
    onCreateHabit: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenHabits: () -> Unit,
    onHabitPrimaryAction: (String, HabitQuickActionType, Boolean) -> Unit,
    onHabitSecondaryAction: (String, HabitQuickActionType) -> Unit,
    events: Flow<TodayUiEvent>,
    onUndo: (String) -> Unit,
    onUndoExpired: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var isQuickCreateExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is TodayUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is TodayUiEvent.ShowUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "撤销",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        onUndo(event.tokenId)
                    } else {
                        onUndoExpired(event.tokenId)
                    }
                }
            }
        }
    }
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            TodayQuickCreateFab(
                expanded = isQuickCreateExpanded,
                onToggleExpanded = { isQuickCreateExpanded = !isQuickCreateExpanded },
                onCreateTask = {
                    isQuickCreateExpanded = false
                    onCreateTask()
                },
                onCreateHabit = {
                    isQuickCreateExpanded = false
                    onCreateHabit()
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TodayHeroCard(
                    title = uiState.title,
                    dateLine = uiState.dateLine,
                    summary = uiState.summary,
                    onOpenSettings = onOpenSettings,
                )
            }
            item {
                NoteFlowStaggeredReveal(revealKey = "today_dual_area", index = 0) {
                    TodayDualColumnQuickArea(
                        uiState = uiState,
                        onOpenTask = onOpenTask,
                        onEditTask = onEditTask,
                        onOpenTasks = onOpenTasks,
                        onTaskAction = onTaskAction,
                        onCreateTask = onCreateTask,
                        onOpenHabit = onOpenHabit,
                        onEditHabit = onEditHabit,
                        onOpenHabits = onOpenHabits,
                        onHabitPrimaryAction = onHabitPrimaryAction,
                        onHabitSecondaryAction = onHabitSecondaryAction,
                        onCreateHabit = onCreateHabit,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(148.dp))
            }
        }
    }
}

@Composable
private fun TodayDualColumnQuickArea(
    uiState: TodayUiState,
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onOpenTasks: () -> Unit,
    onTaskAction: (String, TaskQuickActionType) -> Unit,
    onCreateTask: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenHabits: () -> Unit,
    onHabitPrimaryAction: (String, HabitQuickActionType, Boolean) -> Unit,
    onHabitSecondaryAction: (String, HabitQuickActionType) -> Unit,
    onCreateHabit: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_dual_columns"),
    ) {
        val layoutSpec = rememberTodayCompactLayoutSpec(maxWidth)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(layoutSpec.columnGap),
            verticalAlignment = Alignment.Top,
        ) {
            TodayTasksSection(
                modifier = Modifier
                    .weight(1f)
                    .testTag("today_tasks_column"),
                tasks = uiState.tasks,
                layoutSpec = layoutSpec,
                onOpenTask = onOpenTask,
                onEditTask = onEditTask,
                onOpenTasks = onOpenTasks,
                onTaskAction = onTaskAction,
                onCreateTask = onCreateTask,
            )
            TodayHabitsSection(
                modifier = Modifier
                    .weight(1f)
                    .testTag("today_habits_column"),
                habits = uiState.habits,
                layoutSpec = layoutSpec,
                onOpenHabit = onOpenHabit,
                onEditHabit = onEditHabit,
                onOpenHabits = onOpenHabits,
                onHabitPrimaryAction = onHabitPrimaryAction,
                onHabitSecondaryAction = onHabitSecondaryAction,
                onCreateHabit = onCreateHabit,
            )
        }
    }
}

@Composable
private fun TodayTasksSection(
    modifier: Modifier,
    tasks: List<TodayTaskCardUiModel>,
    layoutSpec: TodayCompactLayoutSpec,
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onOpenTasks: () -> Unit,
    onTaskAction: (String, TaskQuickActionType) -> Unit,
    onCreateTask: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(layoutSpec.cardGap),
    ) {
        TodaySectionHeader(
            title = "今日待办",
            count = tasks.size,
            actionLabel = "查看全部",
            testTag = "today_view_all_tasks",
            layoutSpec = layoutSpec,
            onActionClick = onOpenTasks,
        )
        if (tasks.isEmpty()) {
            TodayEmptySectionCard(
                title = "暂无待办",
                description = "创建一条任务。",
                actionLabel = "新建任务",
                actionTestTag = "today_empty_create_task",
                accentColor = NoteFlowTaskAccent,
                layoutSpec = layoutSpec,
                onActionClick = onCreateTask,
            )
        } else {
            tasks.forEach { task ->
                TodayTaskCard(
                    item = task,
                    layoutSpec = layoutSpec,
                    onClick = { onOpenTask(task.id) },
                    onLongClick = { onEditTask(task.id) },
                    onAction = { onTaskAction(task.id, task.actionType) },
                )
            }
        }
    }
}

@Composable
private fun TodayHabitsSection(
    modifier: Modifier,
    habits: List<TodayHabitCardUiModel>,
    layoutSpec: TodayCompactLayoutSpec,
    onOpenHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenHabits: () -> Unit,
    onHabitPrimaryAction: (String, HabitQuickActionType, Boolean) -> Unit,
    onHabitSecondaryAction: (String, HabitQuickActionType) -> Unit,
    onCreateHabit: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(layoutSpec.cardGap),
    ) {
        TodaySectionHeader(
            title = "今日习惯",
            count = habits.size,
            actionLabel = "查看全部",
            testTag = "today_view_all_habits",
            layoutSpec = layoutSpec,
            onActionClick = onOpenHabits,
        )
        if (habits.isEmpty()) {
            TodayEmptySectionCard(
                title = "暂无习惯",
                description = "创建一个习惯。",
                actionLabel = "新建习惯",
                actionTestTag = "today_empty_create_habit",
                accentColor = NoteFlowHabitAccent,
                layoutSpec = layoutSpec,
                onActionClick = onCreateHabit,
            )
        } else {
            habits.forEach { habit ->
                TodayHabitCard(
                    item = habit,
                    layoutSpec = layoutSpec,
                    onClick = { onOpenHabit(habit.id) },
                    onLongClick = { onEditHabit(habit.id) },
                    onPrimaryAction = {
                        onHabitPrimaryAction(
                            habit.id,
                            habit.primaryActionType,
                            habit.durationRunning,
                        )
                    },
                    onSecondaryAction = {
                        habit.secondaryActionType?.let { onHabitSecondaryAction(habit.id, it) }
                    },
                )
            }
        }
    }
}
