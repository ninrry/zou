package luzzr.zou.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouTodayAccent
import luzzr.zou.core.ui.GlassLevel
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.LayoutTokens
import luzzr.zou.core.ui.ZouPageHeader
import luzzr.zou.core.ui.StandardFieldRow
import luzzr.zou.core.ui.StandardSectionCard
import luzzr.zou.core.ui.StandardSwitchRow
import luzzr.zou.core.ui.noteFlowButtonColors
import luzzr.zou.core.ui.noteFlowOutlinedButtonColors
import luzzr.zou.core.ui.noteFlowOutlinedTextFieldColors

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTaskDefaultIntervalChanged = viewModel::onTaskDefaultIntervalChanged,
        onHabitDefaultIntervalChanged = viewModel::onHabitDefaultIntervalChanged,
        onShowCompletedTasksChanged = viewModel::onShowCompletedTasksChanged,
        onShowOnlyTodayHabitsChanged = viewModel::onShowOnlyTodayHabitsChanged,
        onShowDeletedHabitsChanged = viewModel::onShowDeletedHabitsChanged,
        onSaveDefaults = viewModel::saveDefaultIntervals,
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                },
            )
        },
        onOpenTrash = onOpenTrash,
        onOpenBackup = onOpenBackup,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onTaskDefaultIntervalChanged: (String) -> Unit,
    onHabitDefaultIntervalChanged: (String) -> Unit,
    onShowCompletedTasksChanged: (Boolean) -> Unit,
    onShowOnlyTodayHabitsChanged: (Boolean) -> Unit,
    onShowDeletedHabitsChanged: (Boolean) -> Unit,
    onSaveDefaults: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val saveButtonLabel = when {
        uiState.isSaving -> "保存中"
        uiState.hasPendingChanges -> "保存界面与提醒配置"
        else -> "当前已同步"
    }
    val saveHint = if (uiState.hasPendingChanges) {
        "有未保存的界面配置，保存后会立即同步到待办区和习惯区的显示逻辑。"
    } else {
        "当前配置已同步，列表页会直接按这里的设置显示。"
    }

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_content")
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = LayoutTokens.ScreenHorizontalPadding,
                    vertical = LayoutTokens.ScreenVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space16),
        ) {
            TextButton(
                modifier = Modifier.testTag("settings_back"),
                onClick = onNavigateBack,
                enabled = !uiState.isSaving,
            ) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Text("返回", modifier = Modifier.padding(start = LayoutTokens.Space8))
            }
            ZouPageHeader(
                title = uiState.title,
                subtitle = "统一管理提醒节奏、列表显示和数据工具。",
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.testTag("settings_loading"))
            }

            StandardSectionCard(
                title = "提醒偏好",
                subtitle = "这些配置会同时作用于任务、习惯和今日页的提醒节奏。",
                accentColor = ZouTodayAccent,
            ) {
                StandardFieldRow(label = "任务默认重复提醒间隔（分钟）") {
                    OutlinedTextField(
                        value = uiState.defaultTaskRepeatIntervalText,
                        onValueChange = onTaskDefaultIntervalChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_task_default_interval"),
                        singleLine = true,
                        enabled = !uiState.isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = noteFlowOutlinedTextFieldColors(),
                    )
                }
                StandardFieldRow(label = "习惯默认重复提醒间隔（分钟）") {
                    OutlinedTextField(
                        value = uiState.defaultHabitRepeatIntervalText,
                        onValueChange = onHabitDefaultIntervalChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_habit_default_interval"),
                        singleLine = true,
                        enabled = !uiState.isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = noteFlowOutlinedTextFieldColors(),
                    )
                }
                StandardFieldRow(
                    label = "通知权限",
                    description = if (uiState.notificationPermissionGranted) {
                        "当前已授予通知权限"
                    } else {
                        "当前未授予通知权限"
                    },
                ) {
                    OutlinedButton(
                        onClick = onOpenNotificationSettings,
                        enabled = !uiState.isSaving,
                        colors = noteFlowOutlinedButtonColors(),
                    ) {
                        Text("系统设置")
                    }
                }
            }

            StandardSectionCard(
                title = "显示偏好",
                subtitle = "列表筛选统一收拢到这里，不再占用待办区和习惯区顶部空间。",
                accentColor = ZouTodayAccent,
            ) {
                StandardSwitchRow(
                    title = "待办显示已完成任务",
                    description = "开启后，待办列表会保留已完成任务。",
                    checked = uiState.showCompletedTasks,
                    onCheckedChange = onShowCompletedTasksChanged,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("settings_show_completed_tasks"),
                )
                StandardSwitchRow(
                    title = "习惯仅看今日应执行",
                    description = "只显示今天命中频率规则的习惯，已完成项仍保留显示。",
                    checked = uiState.showOnlyTodayHabits,
                    onCheckedChange = onShowOnlyTodayHabitsChanged,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("settings_show_today_habits"),
                )
                StandardSwitchRow(
                    title = "习惯显示已删除",
                    description = "在习惯列表中显示已软删除项，便于恢复。",
                    checked = uiState.showDeletedHabits,
                    onCheckedChange = onShowDeletedHabitsChanged,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("settings_show_deleted_habits"),
                )
            }

            StandardSectionCard(
                title = "配置同步",
                subtitle = "只在有变更时保存，避免重复操作。",
                accentColor = ZouTodayAccent,
            ) {
                Text(
                    text = saveHint,
                    modifier = Modifier.testTag("settings_save_hint"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.defaultsError?.let {
                    Text(
                        text = it,
                        modifier = Modifier.testTag("settings_defaults_error"),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_save_defaults"),
                    enabled = uiState.hasPendingChanges && !uiState.isSaving && !uiState.isLoading,
                    onClick = onSaveDefaults,
                    colors = noteFlowButtonColors(ZouTodayAccent),
                ) {
                    Text(saveButtonLabel)
                }
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        modifier = Modifier.testTag("settings_error"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.resultMessage?.let {
                    Text(
                        text = it,
                        modifier = Modifier.testTag("settings_result"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            StandardSectionCard(
                title = "数据管理",
                accentColor = ZouTodayAccent,
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_open_trash"),
                    onClick = onOpenTrash,
                    enabled = !uiState.isSaving,
                    colors = noteFlowOutlinedButtonColors(),
                ) {
                    Text("打开回收站")
                }
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_open_backup"),
                    onClick = onOpenBackup,
                    enabled = !uiState.isSaving,
                    colors = noteFlowOutlinedButtonColors(),
                ) {
                    Text("备份与恢复")
                }
            }
        }
    }
}

@Composable
fun TopLevelSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier.testTag("open_settings"),
        shape = RoundedCornerShape(20.dp),
        accentColor = ZouTodayAccent,
        level = GlassLevel.Weak,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "设置",
                tint = ZouTodayAccent.copy(alpha = 0.94f),
            )
        }
    }
}
