package luzzr.zou.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import luzzr.zou.core.designsystem.theme.ZouTodayAccent
import luzzr.zou.core.hyperos.XiaomiPowerKeeper
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    // 每次从外部设置页返回时自动刷新检测状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshOptimizationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTaskDefaultIntervalChanged = viewModel::onTaskDefaultIntervalChanged,
        onHabitDefaultIntervalChanged = viewModel::onHabitDefaultIntervalChanged,
        onShowCompletedTasksChanged = viewModel::onShowCompletedTasksChanged,
        onShowOnlyTodayHabitsChanged = viewModel::onShowOnlyTodayHabitsChanged,
        onShowDeletedHabitsChanged = viewModel::onShowDeletedHabitsChanged,
        onSaveDefaults = viewModel::saveDefaultIntervals,
        onHyperOsOptimizationDone = viewModel::onHyperOsOptimizationDone,
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                },
            )
        },
        onOpenBatterySettings = { XiaomiPowerKeeper.openBatteryOptimizationSettings(context) },
        onOpenAutoStart = { XiaomiPowerKeeper.openAutoStartSettings(context) },
        onOpenLockScreenSettings = { XiaomiPowerKeeper.openNotificationChannelSettings(context) },
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
    onHyperOsOptimizationDone: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit = {},
    onOpenAutoStart: () -> Unit = {},
    onOpenLockScreenSettings: () -> Unit = {},
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

            // ── HyperOS 优化引导（仅小米系设备、未完成时显示） ──────
            if (uiState.isHyperOS && !uiState.hyperOsOptimizationDone) {
                HyperOsOptimizationCard(
                    onOpenBatterySettings = onOpenBatterySettings,
                    onOpenAutoStart = onOpenAutoStart,
                    onOpenLockScreenSettings = onOpenLockScreenSettings,
                    onDismiss = onHyperOsOptimizationDone,
                    batteryOptOk = uiState.optimizeStatus.batteryOptOk,
                    exactAlarmOk = uiState.optimizeStatus.exactAlarmOk,
                )
            }

            // ── 提醒偏好 ──────────────────────────────────────────
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

            // ── 显示偏好 ──────────────────────────────────────────
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

            // ── 配置同步 ──────────────────────────────────────────
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

            // ── 数据管理 ──────────────────────────────────────────
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

// ── HyperOS 优化引导卡片 ──────────────────────────────────────

@Composable
private fun HyperOsOptimizationCard(
    onOpenBatterySettings: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onOpenLockScreenSettings: () -> Unit,
    onDismiss: () -> Unit,
    batteryOptOk: Boolean = false,
    exactAlarmOk: Boolean = false,
) {
    GlassSurface(
        modifier = Modifier.testTag("settings_hyperos_optimization"),
        shape = RoundedCornerShape(20.dp),
        accentColor = Color(0xFFFF6B35),
        level = GlassLevel.Strong,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "澎湃OS 提醒优化",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "为确保提醒准时弹出，建议完成以下设置：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OptimizeActionRow(
                icon = Icons.Outlined.BatterySaver,
                label = "省电策略 → 无限制",
                desc = "防止系统自动限制后台提醒",
                onClick = onOpenBatterySettings,
                done = batteryOptOk,
            )
            OptimizeActionRow(
                icon = Icons.Outlined.PlayArrow,
                label = "允许自启动",
                desc = "重启手机后仍可接收提醒广播",
                onClick = onOpenAutoStart,
                done = false, // 无法自动检测
            )
            OptimizeActionRow(
                icon = Icons.Outlined.Lock,
                label = "锁屏通知 → 显示所有内容",
                desc = "锁屏时也能看到提醒详情",
                onClick = onOpenLockScreenSettings,
                done = false, // 无法自动检测
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = if (batteryOptOk) "✅ 省电策略已就绪" else "🔔 完成上方 3 项设置后点击下方按钮",
                style = MaterialTheme.typography.bodySmall,
                color = if (batteryOptOk) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss,
                colors = noteFlowButtonColors(ZouTodayAccent),
            ) {
                Text("已完成设置，不再提示")
            }
        }
    }
}

@Composable
private fun OptimizeActionRow(
    icon: ImageVector,
    label: String,
    desc: String,
    onClick: () -> Unit,
    done: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (done) {
            Text("✅", modifier = Modifier.size(20.dp))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onClick,
            contentPadding = ButtonDefaults.TextButtonContentPadding,
            colors = noteFlowOutlinedButtonColors(),
        ) {
            Text("设置", style = MaterialTheme.typography.labelSmall)
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
