package luzzr.zou.feature.tasks

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.ui.ZouDateTimeSheet
import luzzr.zou.core.ui.ZouDateTimeSheetMode
import luzzr.zou.core.ui.ZouEditorSection
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouPageHeader
import luzzr.zou.core.ui.ZouStepBar
import luzzr.zou.core.ui.ZouStepBottomBar
import luzzr.zou.core.ui.ZouShimmerList
import luzzr.zou.core.ui.LayoutTokens
import luzzr.zou.core.ui.StandardFieldRow
import luzzr.zou.core.ui.StandardSwitchRow
import luzzr.zou.core.ui.noteFlowButtonColors
import luzzr.zou.core.ui.noteFlowCheckboxColors
import luzzr.zou.core.ui.noteFlowFilterChipColors
import luzzr.zou.core.ui.noteFlowOutlinedButtonColors
import luzzr.zou.core.ui.noteFlowOutlinedTextFieldColors
import luzzr.zou.domain.model.SubTask
import luzzr.zou.domain.model.TaskCompletionRule
import luzzr.zou.domain.model.TaskPriority
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val taskEditorSteps = listOf("基本信息", "时间提醒", "完成方式")

@Composable
fun TaskEditorRoute(
    onNavigateBack: () -> Unit,
    viewModel: TaskEditorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    BackHandler(enabled = !uiState.isSaving) {
        onNavigateBack()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.saveTask(
            onSaved = {
                if (!granted) {
                    Toast.makeText(context, "未授予通知权限，提醒不会发送。", Toast.LENGTH_SHORT).show()
                }
                onNavigateBack()
            },
        )
    }
    val handleSave = {
        if (!viewModel.validateBeforeSave()) {
            Unit
        } else if (uiState.hasReminderConfig && context.shouldRequestNotificationPermission()) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.saveTask(onSaved = onNavigateBack)
        }
    }

    TaskEditorScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTitleChanged = viewModel::onTitleChanged,
        onContentChanged = viewModel::onContentChanged,
        onPrioritySelected = viewModel::onPrioritySelected,
        onUrgentChanged = viewModel::onUrgentChanged,
        onCompletionRuleSelected = viewModel::onCompletionRuleSelected,
        onTaskCompletedChanged = viewModel::onTaskCompletedChanged,
        onDueDateSelected = viewModel::onDueDateSelected,
        onDueTimeSelected = viewModel::onDueTimeSelected,
        onClearDueAt = viewModel::onDueCleared,
        onStartReminderMinuteChanged = viewModel::onStartReminderMinuteChanged,
        onWindowEndMinuteChanged = viewModel::onWindowEndMinuteChanged,
        onRepeatIntervalChanged = viewModel::onRepeatIntervalChanged,
        onAddExactReminder = viewModel::onAddExactReminder,
        onRemoveExactReminder = viewModel::onRemoveExactReminder,
        onReminderNotificationTitleChanged = viewModel::onReminderNotificationTitleChanged,
        onReminderNotificationBodyChanged = viewModel::onReminderNotificationBodyChanged,
        onReminderActiveFromChanged = viewModel::onReminderActiveFromChanged,
        onReminderActiveToChanged = viewModel::onReminderActiveToChanged,
        onAddSubTask = viewModel::onAddSubTask,
        onSubTaskTitleChanged = viewModel::onSubTaskTitleChanged,
        onSubTaskCompletedChanged = viewModel::onSubTaskCompletedChanged,
        onRemoveSubTask = viewModel::onRemoveSubTask,
        onSaveClicked = handleSave,
        onDeleteClicked = { viewModel.onDeleteClicked(onDeleted = onNavigateBack) },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskEditorScreen(
    uiState: TaskEditorUiState,
    onNavigateBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onUrgentChanged: (Boolean) -> Unit,
    onCompletionRuleSelected: (TaskCompletionRule) -> Unit,
    onTaskCompletedChanged: (Boolean) -> Unit,
    onDueDateSelected: (Int, Int, Int) -> Unit,
    onDueTimeSelected: (Int, Int) -> Unit,
    onClearDueAt: () -> Unit,
    onStartReminderMinuteChanged: (Int?) -> Unit,
    onWindowEndMinuteChanged: (Int?) -> Unit,
    onRepeatIntervalChanged: (String) -> Unit,
    onAddExactReminder: (Long) -> Unit,
    onRemoveExactReminder: (Long) -> Unit,
    onReminderNotificationTitleChanged: (String) -> Unit,
    onReminderNotificationBodyChanged: (String) -> Unit,
    onReminderActiveFromChanged: (Long?) -> Unit,
    onReminderActiveToChanged: (Long?) -> Unit,
    onAddSubTask: () -> Unit,
    onSubTaskTitleChanged: (String, String) -> Unit,
    onSubTaskCompletedChanged: (String, Boolean) -> Unit,
    onRemoveSubTask: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onDeleteClicked: () -> Boolean,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val zoneId = remember { ZoneId.systemDefault() }
    val snackbarHostState = remember { SnackbarHostState() }
    var pickerRequest by remember { mutableStateOf<TaskDateTimePickerRequest?>(null) }
    val pagerState = rememberPagerState(pageCount = { taskEditorSteps.size })
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val currentStep = pagerState.currentPage

    LaunchedEffect(uiState.taskId) {
        pagerState.scrollToPage(0)
    }

    LaunchedEffect(uiState.saveErrorMessage) {
        uiState.saveErrorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Top,
                ) {
                    ZouShimmerList()
                }
            }

            uiState.hasMissingContent -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextButton(onClick = onNavigateBack) { Text("返回") }
                    ZouEmptyStateCard(
                        title = "任务不存在",
                        description = uiState.loadErrorMessage ?: "这条任务可能已经被删除。",
                        accentColor = ZouTaskAccent,
                        actionLabel = "返回列表",
                        actionTestTag = "task_editor_go_back",
                        onActionClick = onNavigateBack,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = 0.dp
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {

                    ZouStepBar(
                        steps = taskEditorSteps,
                        currentStep = currentStep,
                        accentColor = ZouTaskAccent,
                        onStepSelected = { step ->
                            if (!uiState.isSaving) {
                                scope.launch { pagerState.animateScrollToPage(step) }
                            }
                        },
                    )

                    // 🌟 使用 Box 包裹 HorizontalPager，实现底部按钮稳定悬浮且内容自由穿透下潜！
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = !uiState.isSaving,
                            beyondViewportPageCount = 1,
                        ) { step ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                when (step) {
                                    0 -> TaskBasicStep(
                                        uiState = uiState,
                                        onTitleChanged = onTitleChanged,
                                        onContentChanged = onContentChanged,
                                        onPrioritySelected = onPrioritySelected,
                                        onUrgentChanged = onUrgentChanged,
                                    )

                                    1 -> TaskScheduleStep(
                                        uiState = uiState,
                                        formatter = formatter,
                                        zoneId = zoneId,
                                        onDueDateSelected = onDueDateSelected,
                                        onDueTimeSelected = onDueTimeSelected,
                                        onClearDueAt = onClearDueAt,
                                        onStartReminderMinuteChanged = onStartReminderMinuteChanged,
                                        onWindowEndMinuteChanged = onWindowEndMinuteChanged,
                                        onRepeatIntervalChanged = onRepeatIntervalChanged,
                                        onAddExactReminder = onAddExactReminder,
                                        onRemoveExactReminder = onRemoveExactReminder,
                                        onReminderNotificationTitleChanged = onReminderNotificationTitleChanged,
                                        onReminderNotificationBodyChanged = onReminderNotificationBodyChanged,
                                        onReminderActiveFromChanged = onReminderActiveFromChanged,
                                        onReminderActiveToChanged = onReminderActiveToChanged,
                                        onShowPicker = { pickerRequest = it },
                                    )

                                    else -> TaskCompletionStep(
                                        uiState = uiState,
                                        onCompletionRuleSelected = onCompletionRuleSelected,
                                        onTaskCompletedChanged = onTaskCompletedChanged,
                                        onAddSubTask = onAddSubTask,
                                        onSubTaskTitleChanged = onSubTaskTitleChanged,
                                        onSubTaskCompletedChanged = onSubTaskCompletedChanged,
                                        onRemoveSubTask = onRemoveSubTask,
                                        onDeleteClicked = onDeleteClicked,
                                    )
                                }

                                // 🌟 垫高垫片，防止底部卡片内容被悬浮按钮遮挡
                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.navigationBarsPadding().height(92.dp)
                                )
                            }
                        }

                        // 🌟 悬浮在 Box 最底部的透明胶囊按键！
                        ZouStepBottomBar(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            primaryLabel = if (uiState.isSaving) {
                                "保存中"
                            } else if (currentStep == taskEditorSteps.lastIndex) {
                                uiState.saveButtonLabel
                            } else {
                                "下一步"
                            },
                            primaryAccentColor = ZouTaskAccent,
                            previousVisible = currentStep > 0,
                            previousEnabled = !uiState.isSaving,
                            primaryEnabled = !uiState.isSaving,
                            primaryLoading = uiState.isSaving,
                            onCancelClick = onNavigateBack,
                            onPreviousClick = {
                                if (!uiState.isSaving) {
                                    scope.launch { pagerState.animateScrollToPage(currentStep - 1) }
                                }
                            },
                            onPrimaryClick = {
                                if (uiState.isSaving) {
                                    Unit
                                } else if (currentStep == taskEditorSteps.lastIndex) {
                                    onSaveClicked()
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(currentStep + 1) }
                                }
                            },
                            primaryTestTag = "task_editor_save",
                        )
                    }
                }
            }
        }

        pickerRequest?.let { request ->
            ZouDateTimeSheet(
                visible = true,
                title = request.title,
                mode = request.mode,
                initialDateTime = request.initialDateTime,
                minimumDateTime = request.minimumDateTime,
                accentColor = ZouTaskAccent,
                onDismissRequest = { pickerRequest = null },
                onConfirm = { date, time, _ ->
                    request.onConfirm(LocalDateTime.of(date, time))
                    pickerRequest = null
                },
            )
        }
    }
}

@Composable
private fun TaskBasicStep(
    uiState: TaskEditorUiState,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onUrgentChanged: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ZouEditorSection(
            title = "任务标题与说明",
            subtitle = "标题负责辨识，正文只补充必要上下文。",
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().testTag("task_editor_title_input"),
                value = uiState.title,
                onValueChange = onTitleChanged,
                label = { Text("标题") },
                singleLine = true,
                isError = uiState.titleError != null,
                supportingText = { uiState.titleError?.let { Text(it) } },
                colors = noteFlowOutlinedTextFieldColors(),
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().testTag("task_editor_content_input"),
                value = uiState.contentMarkdown,
                onValueChange = onContentChanged,
                label = { Text("补充说明") },
                minLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = noteFlowOutlinedTextFieldColors(),
            )
        }

        ZouEditorSection(
            title = "优先级与状态",
            subtitle = "只保留会影响排序和处理节奏的配置。",
        ) {
            StandardFieldRow(label = "优先级") {
                ChoiceRow {
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = uiState.priority == priority,
                            onClick = { onPrioritySelected(priority) },
                            label = {
                                Text(
                                    when (priority) {
                                        TaskPriority.NORMAL -> "普通"
                                        TaskPriority.HIGH -> "高"
                                        TaskPriority.URGENT -> "紧急"
                                    },
                                )
                            },
                            colors = noteFlowFilterChipColors(ZouTaskAccent),
                        )
                    }
                }
            }

            StandardSwitchRow(
                title = "紧急任务",
                description = "打开后会在列表和今日页中更靠前。",
                checked = uiState.isUrgent,
                onCheckedChange = onUrgentChanged,
                accentColor = ZouTaskAccent,
            )
        }
    }
}

@Composable
private fun TaskScheduleStep(
    uiState: TaskEditorUiState,
    formatter: DateTimeFormatter,
    zoneId: ZoneId,
    onDueDateSelected: (Int, Int, Int) -> Unit,
    onDueTimeSelected: (Int, Int) -> Unit,
    onClearDueAt: () -> Unit,
    onStartReminderMinuteChanged: (Int?) -> Unit,
    onWindowEndMinuteChanged: (Int?) -> Unit,
    onRepeatIntervalChanged: (String) -> Unit,
    onAddExactReminder: (Long) -> Unit,
    onRemoveExactReminder: (Long) -> Unit,
    onReminderNotificationTitleChanged: (String) -> Unit,
    onReminderNotificationBodyChanged: (String) -> Unit,
    onReminderActiveFromChanged: (Long?) -> Unit,
    onReminderActiveToChanged: (Long?) -> Unit,
    onShowPicker: (TaskDateTimePickerRequest) -> Unit,
) {
    var showAdvancedReminder by rememberSaveable(uiState.taskId) {
        mutableStateOf(
            uiState.repeatIntervalMinutesText.isNotBlank() ||
                uiState.exactReminderTimes.isNotEmpty() ||
                uiState.reminderNotificationTitle.isNotBlank() ||
                uiState.reminderNotificationBody.isNotBlank(),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ZouEditorSection(
            title = "截止时间",
            subtitle = "先确定完成时点，再决定提醒强度。",
        ) {
            ReadOnlyDateTimeField(
                label = "截止时间",
                value = uiState.dueAt.formatWith(formatter),
                errorMessage = uiState.dueAtError,
            )
            ChoiceRow {
                OutlinedButton(
                    onClick = {
                        val now = LocalDateTime.now(zoneId)
                        val initialDateTime = uiState.dueAt.toLocalDateTime() ?: now
                        onShowPicker(
                            TaskDateTimePickerRequest(
                                title = "选择截止日期",
                                mode = ZouDateTimeSheetMode.DATE_ONLY,
                                initialDateTime = initialDateTime,
                                minimumDateTime = now,
                                onConfirm = { selected -> onDueDateSelected(selected.year, selected.monthValue, selected.dayOfMonth) },
                            ),
                        )
                    },
                    colors = noteFlowOutlinedButtonColors(),
                ) { Text("选择日期") }
                OutlinedButton(
                    onClick = {
                        val now = LocalDateTime.now(zoneId)
                        val initialDateTime = uiState.dueAt.toLocalDateTime() ?: now
                        onShowPicker(
                            TaskDateTimePickerRequest(
                                title = "选择截止时间",
                                mode = ZouDateTimeSheetMode.TIME_ONLY,
                                initialDateTime = initialDateTime,
                                minimumDateTime = now,
                                onConfirm = { selected -> onDueTimeSelected(selected.hour, selected.minute) },
                            ),
                        )
                    },
                    enabled = uiState.dueAt != null,
                    colors = noteFlowOutlinedButtonColors(),
                ) { Text("选择时间") }
                TextButton(onClick = onClearDueAt, enabled = uiState.dueAt != null) { Text("清除") }
            }
        }

        ZouEditorSection(
            title = "提醒窗口",
            subtitle = "只保留当天有效的提醒时间范围。",
        ) {
            StandardFieldRow(label = "开始提醒时间") {
                ReadOnlyDateTimeField(label = "开始提醒时间", value = uiState.startReminderMinuteOfDay.toDisplayTime())
                ChoiceRow {
                    OutlinedButton(
                        modifier = Modifier.testTag("task_editor_start_reminder_button"),
                        onClick = {
                            val initial = uiState.startReminderMinuteOfDay ?: 9 * 60
                            onShowPicker(
                                TaskDateTimePickerRequest(
                                    title = "设置开始提醒",
                                    mode = ZouDateTimeSheetMode.TIME_ONLY,
                                    initialDateTime = LocalDate.now(zoneId).atTime(initial / 60, initial % 60),
                                    onConfirm = { selected -> onStartReminderMinuteChanged(selected.hour * 60 + selected.minute) },
                                ),
                            )
                        },
                        colors = noteFlowOutlinedButtonColors(),
                    ) { Text("设置开始时间") }
                    TextButton(
                        modifier = Modifier.testTag("task_editor_clear_start_reminder"),
                        onClick = { onStartReminderMinuteChanged(null) },
                        enabled = uiState.startReminderMinuteOfDay != null,
                    ) { Text("清除") }
                }
            }

            StandardFieldRow(label = "提醒窗口结束时间") {
                ReadOnlyDateTimeField(label = "提醒窗口结束时间", value = uiState.windowEndMinuteOfDay.toDisplayTime())
                ChoiceRow {
                    OutlinedButton(
                        modifier = Modifier.testTag("task_editor_window_end_button"),
                        onClick = {
                            val initial = uiState.windowEndMinuteOfDay ?: (22 * 60)
                            onShowPicker(
                                TaskDateTimePickerRequest(
                                    title = "设置窗口结束",
                                    mode = ZouDateTimeSheetMode.TIME_ONLY,
                                    initialDateTime = LocalDate.now(zoneId).atTime(initial / 60, initial % 60),
                                    onConfirm = { selected -> onWindowEndMinuteChanged(selected.hour * 60 + selected.minute) },
                                ),
                            )
                        },
                        colors = noteFlowOutlinedButtonColors(),
                    ) { Text("设置结束时间") }
                    TextButton(
                        modifier = Modifier.testTag("task_editor_clear_window_end"),
                        onClick = { onWindowEndMinuteChanged(null) },
                        enabled = uiState.windowEndMinuteOfDay != null,
                    ) { Text("清除") }
                }
            }
        }

        ZouEditorSection(
            title = "提醒日期范围",
            subtitle = "设置后只在此日期范围内发送提醒，范围外自动静默。",
        ) {
            StandardFieldRow(label = "开始提醒日期") {
                ReadOnlyDateTimeField(
                    label = "开始提醒日期",
                    value = uiState.reminderActiveFrom.formatDateWith(zoneId),
                )
                ChoiceRow {
                    OutlinedButton(
                        modifier = Modifier.testTag("task_editor_reminder_from_button"),
                        onClick = {
                            val now = LocalDateTime.now(zoneId)
                            val initial = uiState.reminderActiveFrom?.let {
                                java.time.Instant.ofEpochMilli(it).atZone(zoneId).toLocalDateTime()
                            } ?: now
                            onShowPicker(
                                TaskDateTimePickerRequest(
                                    title = "设置开始提醒日期",
                                    mode = ZouDateTimeSheetMode.DATE_ONLY,
                                    initialDateTime = initial,
                                    onConfirm = { selected ->
                                        onReminderActiveFromChanged(
                                            selected.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                                        )
                                    },
                                ),
                            )
                        },
                        colors = noteFlowOutlinedButtonColors(),
                    ) { Text("选择日期") }
                    TextButton(
                        modifier = Modifier.testTag("task_editor_clear_reminder_from"),
                        onClick = { onReminderActiveFromChanged(null) },
                        enabled = uiState.reminderActiveFrom != null,
                    ) { Text("清除") }
                }
            }

            StandardFieldRow(label = "结束提醒日期") {
                ReadOnlyDateTimeField(
                    label = "结束提醒日期",
                    value = uiState.reminderActiveTo.formatDateWith(zoneId),
                )
                ChoiceRow {
                    OutlinedButton(
                        modifier = Modifier.testTag("task_editor_reminder_to_button"),
                        onClick = {
                            val now = LocalDateTime.now(zoneId)
                            val initial = uiState.reminderActiveTo?.let {
                                java.time.Instant.ofEpochMilli(it).atZone(zoneId).toLocalDateTime()
                            } ?: now
                            onShowPicker(
                                TaskDateTimePickerRequest(
                                    title = "设置结束提醒日期",
                                    mode = ZouDateTimeSheetMode.DATE_ONLY,
                                    initialDateTime = initial,
                                    onConfirm = { selected ->
                                        onReminderActiveToChanged(
                                            selected.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                                        )
                                    },
                                ),
                            )
                        },
                        colors = noteFlowOutlinedButtonColors(),
                    ) { Text("选择日期") }
                    TextButton(
                        modifier = Modifier.testTag("task_editor_clear_reminder_to"),
                        onClick = { onReminderActiveToChanged(null) },
                        enabled = uiState.reminderActiveTo != null,
                    ) { Text("清除") }
                }
            }

            uiState.reminderDateRangeError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        ZouEditorSection(
            title = "高级提醒",
            subtitle = if (showAdvancedReminder) {
                "收起后只保留摘要，适合不常改的提醒配置。"
            } else {
                uiState.advancedReminderSummary()
            },
        ) {
            TextButton(onClick = { showAdvancedReminder = !showAdvancedReminder }) {
                Text(if (showAdvancedReminder) "收起高级提醒" else "展开高级提醒")
            }
            if (showAdvancedReminder) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("task_editor_repeat_interval_input"),
                    value = uiState.repeatIntervalMinutesText,
                    onValueChange = onRepeatIntervalChanged,
                    label = { Text("重复提醒间隔（分钟）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.repeatIntervalError != null,
                    supportingText = { uiState.repeatIntervalError?.let { Text(it) } },
                    colors = noteFlowOutlinedTextFieldColors(),
                )

                StandardFieldRow(label = "特别提醒") {
                    OutlinedButton(
                        modifier = Modifier.testTag("task_editor_add_exact_reminder"),
                        onClick = {
                            val now = LocalDateTime.now(zoneId)
                            onShowPicker(
                                TaskDateTimePickerRequest(
                                    title = "添加特别提醒",
                                    mode = ZouDateTimeSheetMode.DATE_TIME,
                                    initialDateTime = uiState.dueAt.toLocalDateTime() ?: now,
                                    minimumDateTime = now,
                                    onConfirm = { selected -> onAddExactReminder(selected.toEpochMillis(zoneId)) },
                                ),
                            )
                        },
                        colors = noteFlowOutlinedButtonColors(),
                    ) { Text("添加特别提醒") }

                    if (uiState.exactReminderTimes.isEmpty()) {
                        Text(
                            text = "还没有特别提醒。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.exactReminderTimes.forEach { reminderAt ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = reminderAt.formatWith(formatter),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    TextButton(onClick = { onRemoveExactReminder(reminderAt) }) { Text("删除") }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("task_editor_notification_title_input"),
                    value = uiState.reminderNotificationTitle,
                    onValueChange = onReminderNotificationTitleChanged,
                    label = { Text("通知标题（可选）") },
                    singleLine = true,
                    colors = noteFlowOutlinedTextFieldColors(),
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("task_editor_notification_body_input"),
                    value = uiState.reminderNotificationBody,
                    onValueChange = onReminderNotificationBodyChanged,
                    label = { Text("通知正文（可选）") },
                    minLines = 3,
                    maxLines = 4,
                    colors = noteFlowOutlinedTextFieldColors(),
                )
            }

            uiState.reminderError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun TaskCompletionStep(
    uiState: TaskEditorUiState,
    onCompletionRuleSelected: (TaskCompletionRule) -> Unit,
    onTaskCompletedChanged: (Boolean) -> Unit,
    onAddSubTask: () -> Unit,
    onSubTaskTitleChanged: (String, String) -> Unit,
    onSubTaskCompletedChanged: (String, Boolean) -> Unit,
    onRemoveSubTask: (String) -> Unit,
    onDeleteClicked: () -> Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ZouEditorSection(
            title = "完成规则",
            subtitle = "决定任务是手动结束，还是跟随子任务自动完成。",
        ) {
            ChoiceRow {
                TaskCompletionRule.entries.forEach { rule ->
                    FilterChip(
                        selected = uiState.completionRule == rule,
                        onClick = { onCompletionRuleSelected(rule) },
                        label = {
                            Text(
                                when (rule) {
                                    TaskCompletionRule.MANUAL -> "手动完成"
                                    TaskCompletionRule.AUTO_ALL_SUBTASKS -> "子任务自动完成"
                                },
                            )
                        },
                        colors = noteFlowFilterChipColors(ZouTaskAccent),
                    )
                }
            }

            if (uiState.canToggleTaskCompletion) {
                StandardSwitchRow(
                    title = "任务完成状态",
                    description = "可以直接切换完成或恢复完成。",
                    checked = uiState.isCompleted,
                    onCheckedChange = onTaskCompletedChanged,
                    accentColor = ZouTaskAccent,
                )
            } else {
                Text(
                    text = "当前状态由子任务完成情况自动决定。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ZouEditorSection(
            title = "子任务拆解",
            subtitle = "只添加真正会影响完成判断的步骤。",
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "当前步骤",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(modifier = Modifier.testTag("task_editor_add_subtask"), onClick = onAddSubTask) {
                    Text("新增子任务")
                }
            }

            if (uiState.subTasks.isEmpty()) {
                Text(
                    text = "还没有子任务。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.subTasks.forEach { subTask ->
                        SubTaskEditorRow(
                            subTask = subTask,
                            onTitleChanged = { onSubTaskTitleChanged(subTask.id, it) },
                            onCompletedChanged = { onSubTaskCompletedChanged(subTask.id, it) },
                            onRemoveClicked = { onRemoveSubTask(subTask.id) },
                        )
                    }
                }
            }
        }

        if (uiState.canDelete) {
            ZouEditorSection(
                title = "危险操作",
                subtitle = "删除后会进入回收站，不影响底部主动作区。",
            ) {
                TextButton(
                    modifier = Modifier.fillMaxWidth().testTag("task_editor_delete"),
                    onClick = { onDeleteClicked() },
                    enabled = !uiState.isSaving,
                ) {
                    Text(text = "软删除任务", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        uiState.saveErrorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                modifier = Modifier.testTag("task_editor_save_error"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ChoiceRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun ReadOnlyDateTimeField(
    label: String,
    value: String,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        isError = errorMessage != null,
        supportingText = { errorMessage?.let { Text(it) } },
        colors = noteFlowOutlinedTextFieldColors(),
    )
}

@Composable
private fun SubTaskEditorRow(
    subTask: SubTask,
    onTitleChanged: (String) -> Unit,
    onCompletedChanged: (Boolean) -> Unit,
    onRemoveClicked: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.material3.Checkbox(
            modifier = Modifier.testTag("subtask_completion_toggle"),
            checked = subTask.isCompleted,
            onCheckedChange = onCompletedChanged,
            colors = noteFlowCheckboxColors(ZouTaskAccent),
        )
        OutlinedTextField(
            modifier = Modifier.weight(1f).testTag("subtask_title_input"),
            value = subTask.title,
            onValueChange = onTitleChanged,
            label = { Text("子任务标题") },
            singleLine = true,
            colors = noteFlowOutlinedTextFieldColors(),
        )
        TextButton(onClick = onRemoveClicked) { Text("删除") }
    }
}

private data class TaskDateTimePickerRequest(
    val title: String,
    val mode: ZouDateTimeSheetMode,
    val initialDateTime: LocalDateTime,
    val minimumDateTime: LocalDateTime? = null,
    val onConfirm: (LocalDateTime) -> Unit,
)

private fun Long?.formatWith(formatter: DateTimeFormatter): String {
    return this?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(formatter)
    } ?: "未设置"
}

private fun Long?.toLocalDateTime(): LocalDateTime? {
    return this?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
}

private fun Int?.toDisplayTime(): String {
    if (this == null) return "未设置"
    val hour = this / 60
    val minute = this % 60
    return "%02d:%02d".format(hour, minute)
}

private fun TaskEditorUiState.advancedReminderSummary(): String {
    val parts = buildList {
        repeatIntervalMinutesText.toIntOrNull()?.takeIf { it > 0 }?.let { add("每 $it 分钟重复") }
        if (exactReminderTimes.isNotEmpty()) add("${exactReminderTimes.size} 个特别提醒")
        if (reminderNotificationTitle.isNotBlank() || reminderNotificationBody.isNotBlank()) add("自定义通知文案")
    }
    return if (parts.isEmpty()) {
        "默认收起重复提醒、特别提醒和通知文案。"
    } else {
        parts.joinToString("，")
    }
}

private fun LocalDateTime.toEpochMillis(zoneId: ZoneId): Long {
    return atZone(zoneId).toInstant().toEpochMilli()
}

private fun Context.shouldRequestNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return false
    }
    return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}

private fun Long?.formatDateWith(zoneId: ZoneId): String {
    if (this == null) return "未设置"
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    return java.time.Instant.ofEpochMilli(this).atZone(zoneId).format(dateFormatter)
}
