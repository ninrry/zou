package luzzr.zou.feature.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouHabitAccent
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouPageHeader
import luzzr.zou.core.ui.ZouSectionCard

@Composable
fun HabitDetailRoute(
    onNavigateBack: () -> Unit,
    onEditHabit: (String) -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    HabitDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEditHabit = onEditHabit,
        onStepCheckedChanged = viewModel::onStepCheckedChanged,
        onDurationTimerToggle = viewModel::onDurationTimerToggle,
        onDurationFinish = viewModel::onDurationFinish,
        onCompleteClicked = viewModel::onCompleteClicked,
        onDeleteClicked = { viewModel.onDeleteClicked(onDeleted = onNavigateBack) },
    )
}

@Composable
fun HabitDetailScreen(
    uiState: HabitDetailUiState,
    onNavigateBack: () -> Unit,
    onEditHabit: (String) -> Unit,
    onStepCheckedChanged: (String, Boolean) -> Unit,
    onDurationTimerToggle: () -> Unit,
    onDurationFinish: () -> Unit,
    onCompleteClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    ScaffoldBody(uiState, onNavigateBack) {
        ZouPageHeader(
            title = uiState.title,
            subtitle = "按模式完成打卡：勾选型一键完成，步骤型完成全部步骤，时长型先计时再结束。",
        )
        ZouSectionCard(title = "当前状态") {
            DetailLine("今日状态", uiState.todayStatusText)
            DetailLine("频率规则", uiState.frequencyText)
            DetailLine("打卡模式", uiState.modeText)
            Text(
                text = uiState.helperText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!uiState.actionMessage.isNullOrBlank()) {
            Text(
                text = uiState.actionMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (uiState.showStepsSection) {
            ZouSectionCard(title = "步骤") {
                if (uiState.steps.isEmpty()) {
                    Text(
                        text = "步骤为空，无法完成步骤型打卡。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.steps.forEach { step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = uiState.canCheckInToday,
                                onClick = { onStepCheckedChanged(step.id, !step.checked) },
                            )
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = step.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (step.checked) "已完成" else "未完成",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (step.checked) {
                                ZouHabitAccent
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }

        if (uiState.showDurationSection) {
            ZouSectionCard(title = "时长打卡") {
                Text(
                    text = uiState.durationTargetText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.durationElapsedText,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("habit_duration_timer_toggle"),
                        enabled = uiState.canCheckInToday,
                        onClick = onDurationTimerToggle,
                    ) {
                        Text(uiState.durationTimerActionLabel)
                    }
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("habit_duration_finish"),
                        enabled = uiState.durationCanFinish,
                        onClick = onDurationFinish,
                    ) {
                        Text("结束并打卡")
                    }
                }
            }
        }

        if (uiState.shouldShowMainCompleteAction) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_detail_complete"),
                enabled = uiState.completeEnabled,
                onClick = onCompleteClicked,
            ) {
                Text(uiState.completeButtonLabel)
            }
        }

        if (uiState.canShowContent) {
            ZouSectionCard(title = "详情") {
                Text(
                    text = uiState.contentMarkdown,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.canEdit) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_detail_edit"),
                onClick = { uiState.habitId?.let(onEditHabit) },
            ) {
                Text("编辑习惯")
            }
        }
        if (uiState.canDelete) {
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_detail_delete"),
                onClick = onDeleteClicked,
            ) {
                Text(
                    text = "软删除习惯",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ScaffoldBody(
    uiState: HabitDetailUiState,
    onNavigateBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Scaffold(containerColor = Color.Transparent) { innerPadding ->
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
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("返回")
                    }
                    ZouEmptyStateCard(
                        title = "习惯不存在或已删除",
                        description = uiState.emptyMessage,
                        accentColor = ZouHabitAccent,
                        actionLabel = "返回列表",
                        actionTestTag = "habit_detail_go_back",
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("返回")
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
