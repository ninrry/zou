package luzzr.zou.feature.tasks

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouPageHeader
import luzzr.zou.core.ui.ZouSectionCard

@Composable
fun TaskDetailRoute(
    onNavigateBack: () -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    TaskDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEditTask = onEditTask,
    )
}

@Composable
fun TaskDetailScreen(
    uiState: TaskDetailUiState,
    onNavigateBack: () -> Unit,
    onEditTask: (String) -> Unit,
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
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("返回")
                    }
                    ZouEmptyStateCard(
                        title = "任务不存在或已删除",
                        description = uiState.emptyMessage,
                        accentColor = ZouTaskAccent,
                        actionLabel = "返回列表",
                        actionTestTag = "task_detail_go_back",
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
                    ZouPageHeader(
                        title = uiState.title,
                        subtitle = "详情只读展示，编辑与删除请进入编辑页。",
                    )
                    ZouSectionCard(title = "当前状态") {
                        DetailLine("状态", uiState.statusText)
                        DetailLine("截止时间", uiState.dueText)
                        DetailLine("子任务进度", uiState.progressText)
                    }
                    ZouSectionCard(title = "提醒设置") {
                        uiState.reminderSummary.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    ZouSectionCard(title = "详情") {
                        Text(
                            text = if (uiState.contentMarkdown.isBlank()) "暂无详情内容" else uiState.contentMarkdown,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (uiState.canEdit) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_detail_edit"),
                            onClick = { uiState.taskId?.let(onEditTask) },
                        ) {
                            Text("编辑任务")
                        }
                    }
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
