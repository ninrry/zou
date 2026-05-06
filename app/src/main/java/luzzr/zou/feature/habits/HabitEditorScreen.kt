package luzzr.zou.feature.habits

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import luzzr.zou.core.designsystem.theme.ZouHabitAccent
import luzzr.zou.core.ui.ZouDateTimeSheet
import luzzr.zou.core.ui.ZouDateTimeSheetMode
import luzzr.zou.core.ui.ZouEditorSection
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouPageHeader
import luzzr.zou.core.ui.ZouStepBar
import luzzr.zou.core.ui.ZouStepBottomBar
import luzzr.zou.core.ui.StandardFieldRow
import luzzr.zou.core.ui.noteFlowButtonColors
import luzzr.zou.core.ui.noteFlowFilterChipColors
import luzzr.zou.core.ui.noteFlowOutlinedButtonColors
import luzzr.zou.core.ui.noteFlowOutlinedTextFieldColors
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitStep
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch

private val habitEditorSteps = listOf("基本信息", "频率规则", "提醒与步骤")

@Composable
fun HabitEditorRoute(
    onNavigateBack: () -> Unit,
    viewModel: HabitEditorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    BackHandler(enabled = !uiState.isSaving) {
        onNavigateBack()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.saveHabit {
            if (!granted) {
                Toast.makeText(context, "未授予通知权限，提醒不会发送。", Toast.LENGTH_SHORT).show()
            }
            onNavigateBack()
        }
    }
    val handleSave = {
        if (!viewModel.validateBeforeSave()) {
            Unit
        } else if (uiState.hasReminderConfig && context.shouldRequestNotificationPermission()) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.saveHabit(onSaved = onNavigateBack)
        }
    }

    HabitEditorScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTitleChanged = viewModel::onTitleChanged,
        onContentChanged = viewModel::onContentChanged,
        onFrequencySelected = viewModel::onFrequencySelected,
        onWeekdayToggled = viewModel::onWeekdayToggled,
        onIntervalDaysChanged = viewModel::onIntervalDaysChanged,
        onIntervalAnchorDateChanged = viewModel::onIntervalAnchorDateChanged,
        onMonthlyDaysChanged = viewModel::onMonthlyDaysChanged,
        onRemindWindowStartChanged = viewModel::onRemindWindowStartChanged,
        onRemindWindowEndChanged = viewModel::onRemindWindowEndChanged,
        onRepeatIntervalChanged = viewModel::onRepeatIntervalChanged,
        onAddExactReminder = viewModel::onAddExactReminder,
        onRemoveExactReminder = viewModel::onRemoveExactReminder,
        onReminderNotificationTitleChanged = viewModel::onReminderNotificationTitleChanged,
        onReminderNotificationBodyChanged = viewModel::onReminderNotificationBodyChanged,
        onCheckInModeSelected = viewModel::onCheckInModeSelected,
        onTargetDurationChanged = viewModel::onTargetDurationChanged,
        onAddStep = viewModel::onAddStep,
        onStepTitleChanged = viewModel::onStepTitleChanged,
        onRemoveStep = viewModel::onRemoveStep,
        onSaveClicked = handleSave,
        onDeleteClicked = { viewModel.onDeleteClicked(onDeleted = onNavigateBack) },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitEditorScreen(
    uiState: HabitEditorUiState,
    onNavigateBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onFrequencySelected: (HabitFrequencyType) -> Unit,
    onWeekdayToggled: (DayOfWeek) -> Unit,
    onIntervalDaysChanged: (String) -> Unit,
    onIntervalAnchorDateChanged: (LocalDate) -> Unit,
    onMonthlyDaysChanged: (String) -> Unit,
    onRemindWindowStartChanged: (LocalTime?) -> Unit,
    onRemindWindowEndChanged: (LocalTime?) -> Unit,
    onRepeatIntervalChanged: (String) -> Unit,
    onAddExactReminder: (LocalTime) -> Unit,
    onRemoveExactReminder: (LocalTime) -> Unit,
    onReminderNotificationTitleChanged: (String) -> Unit,
    onReminderNotificationBodyChanged: (String) -> Unit,
    onCheckInModeSelected: (HabitCheckInMode) -> Unit,
    onTargetDurationChanged: (String) -> Unit,
    onAddStep: () -> Unit,
    onStepTitleChanged: (String, String) -> Unit,
    onRemoveStep: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val zoneId = remember { ZoneId.systemDefault() }
    val snackbarHostState = remember { SnackbarHostState() }
    var pickerRequest by remember { mutableStateOf<HabitDateTimePickerRequest?>(null) }
    val pagerState = rememberPagerState(pageCount = { habitEditorSteps.size })
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val currentStep = pagerState.currentPage

    LaunchedEffect(uiState.habitId) {
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
        bottomBar = {
            if (!uiState.isLoading && !uiState.hasMissingContent) {
                ZouStepBottomBar(
                    primaryLabel = if (uiState.isSaving) {
                        "保存中"
                    } else if (currentStep == habitEditorSteps.lastIndex) {
                        uiState.saveButtonLabel
                    } else {
                        "下一步"
                    },
                    primaryAccentColor = ZouHabitAccent,
                    previousVisible = currentStep > 0,
                    previousEnabled = !uiState.isSaving,
                    primaryEnabled = !uiState.isSaving,
                    primaryLoading = uiState.isSaving,
                    onPreviousClick = {
                        if (!uiState.isSaving) {
                            scope.launch { pagerState.animateScrollToPage(currentStep - 1) }
                        }
                    },
                    onPrimaryClick = {
                        if (uiState.isSaving) {
                            Unit
                        } else if (currentStep == habitEditorSteps.lastIndex) {
                            onSaveClicked()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(currentStep + 1) }
                        }
                    },
                    primaryTestTag = "habit_editor_save",
                )
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp))
            }
        } else if (uiState.hasMissingContent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(onClick = onNavigateBack) { Text("返回") }
                ZouEmptyStateCard(
                    title = "习惯不存在",
                    description = uiState.loadErrorMessage ?: "这条习惯可能已经被删除。",
                    accentColor = ZouHabitAccent,
                    actionLabel = "返回列表",
                    actionTestTag = "habit_editor_go_back",
                    onActionClick = onNavigateBack,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                TextButton(onClick = onNavigateBack, enabled = !uiState.isSaving) { Text("返回") }
                ZouPageHeader(
                    title = uiState.screenTitle,
                    subtitle = "先定义执行方式，再设置频率和提醒。",
                )
                ZouStepBar(
                    steps = habitEditorSteps,
                    currentStep = currentStep,
                    accentColor = ZouHabitAccent,
                    onStepSelected = { step ->
                        if (!uiState.isSaving) {
                            scope.launch { pagerState.animateScrollToPage(step) }
                        }
                    },
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                            0 -> HabitBasicStep(
                                uiState = uiState,
                                onTitleChanged = onTitleChanged,
                                onContentChanged = onContentChanged,
                                onCheckInModeSelected = onCheckInModeSelected,
                            )

                            1 -> HabitFrequencyStep(
                                uiState = uiState,
                                dateFormatter = dateFormatter,
                                zoneId = zoneId,
                                onFrequencySelected = onFrequencySelected,
                                onWeekdayToggled = onWeekdayToggled,
                                onIntervalDaysChanged = onIntervalDaysChanged,
                                onIntervalAnchorDateChanged = onIntervalAnchorDateChanged,
                                onMonthlyDaysChanged = onMonthlyDaysChanged,
                                onShowPicker = { pickerRequest = it },
                            )

                            else -> HabitReminderStep(
                                uiState = uiState,
                                timeFormatter = timeFormatter,
                                zoneId = zoneId,
                                onRemindWindowStartChanged = onRemindWindowStartChanged,
                                onRemindWindowEndChanged = onRemindWindowEndChanged,
                                onRepeatIntervalChanged = onRepeatIntervalChanged,
                                onAddExactReminder = onAddExactReminder,
                                onRemoveExactReminder = onRemoveExactReminder,
                                onReminderNotificationTitleChanged = onReminderNotificationTitleChanged,
                                onReminderNotificationBodyChanged = onReminderNotificationBodyChanged,
                                onTargetDurationChanged = onTargetDurationChanged,
                                onAddStep = onAddStep,
                                onStepTitleChanged = onStepTitleChanged,
                                onRemoveStep = onRemoveStep,
                                onDeleteClicked = onDeleteClicked,
                                onShowPicker = { pickerRequest = it },
                            )
                        }
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
                accentColor = ZouHabitAccent,
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
private fun HabitBasicStep(
    uiState: HabitEditorUiState,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onCheckInModeSelected: (HabitCheckInMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ZouEditorSection(
            title = "习惯标题与说明",
            subtitle = "标题说明要做什么，正文只保留执行提示。",
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().testTag("habit_editor_title_input"),
                value = uiState.title,
                onValueChange = onTitleChanged,
                label = { Text("标题") },
                singleLine = true,
                isError = uiState.titleError != null,
                supportingText = { uiState.titleError?.let { Text(it) } },
                colors = noteFlowOutlinedTextFieldColors(),
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.contentMarkdown,
                onValueChange = onContentChanged,
                label = { Text("补充说明") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = noteFlowOutlinedTextFieldColors(),
            )
        }

        ZouEditorSection(
            title = "执行方式",
            subtitle = "决定今天如何算完成，后续字段会跟随这个模式变化。",
        ) {
            StandardFieldRow(label = "打卡模式") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitCheckInMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.checkInMode == mode,
                            onClick = { onCheckInModeSelected(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        HabitCheckInMode.CHECK -> "勾选"
                                        HabitCheckInMode.STEPS -> "步骤"
                                        HabitCheckInMode.DURATION -> "时长"
                                    },
                                )
                            },
                            colors = noteFlowFilterChipColors(ZouHabitAccent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitFrequencyStep(
    uiState: HabitEditorUiState,
    dateFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onFrequencySelected: (HabitFrequencyType) -> Unit,
    onWeekdayToggled: (DayOfWeek) -> Unit,
    onIntervalDaysChanged: (String) -> Unit,
    onIntervalAnchorDateChanged: (LocalDate) -> Unit,
    onMonthlyDaysChanged: (String) -> Unit,
    onShowPicker: (HabitDateTimePickerRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ZouEditorSection(
            title = "频率类型",
            subtitle = uiState.frequencySummary(),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HabitFrequencyType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.frequencyType == type,
                        onClick = { onFrequencySelected(type) },
                        label = {
                            Text(
                                when (type) {
                                    HabitFrequencyType.DAILY -> "每日"
                                    HabitFrequencyType.WEEKLY -> "每周"
                                    HabitFrequencyType.INTERVAL_DAYS -> "间隔天"
                                    HabitFrequencyType.MONTHLY -> "每月"
                                },
                            )
                        },
                        colors = noteFlowFilterChipColors(ZouHabitAccent),
                    )
                }
            }
        }

        ZouEditorSection(
            title = "频率细节",
            subtitle = "只展示当前频率类型需要的字段。",
        ) {
            when (uiState.frequencyType) {
                HabitFrequencyType.DAILY -> {
                    Text(
                        text = "每天执行一次。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HabitFrequencyType.WEEKLY -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayOfWeek.entries.forEach { dayOfWeek ->
                            FilterChip(
                                selected = uiState.selectedWeekdays.contains(dayOfWeek),
                                onClick = { onWeekdayToggled(dayOfWeek) },
                                label = { Text(dayOfWeekLabel(dayOfWeek)) },
                                colors = noteFlowFilterChipColors(ZouHabitAccent),
                            )
                        }
                    }
                }
                HabitFrequencyType.INTERVAL_DAYS -> {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.intervalDaysText,
                        onValueChange = onIntervalDaysChanged,
                        label = { Text("间隔天数") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = noteFlowOutlinedTextFieldColors(),
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.intervalAnchorDate?.format(dateFormatter) ?: "未设置",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("起始日期") },
                        colors = noteFlowOutlinedTextFieldColors(),
                    )
                    OutlinedButton(
                        onClick = {
                            onShowPicker(
                                HabitDateTimePickerRequest(
                                    title = "选择起始日期",
                                    mode = ZouDateTimeSheetMode.DATE_ONLY,
                                    initialDateTime = uiState.intervalAnchorDate?.atTime(9, 0) ?: LocalDateTime.now(zoneId),
                                    onConfirm = { selected -> onIntervalAnchorDateChanged(selected.toLocalDate()) },
                                ),
                            )
                        },
                        colors = noteFlowOutlinedButtonColors(),
                    ) { Text("选择起始日期") }
                }
                HabitFrequencyType.MONTHLY -> {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.monthlyDaysText,
                        onValueChange = onMonthlyDaysChanged,
                        label = { Text("每月日期（逗号分隔）") },
                        supportingText = { Text("例如：1,15,31") },
                        colors = noteFlowOutlinedTextFieldColors(),
                    )
                }
            }
            uiState.frequencyError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun HabitReminderStep(
    uiState: HabitEditorUiState,
    timeFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onRemindWindowStartChanged: (LocalTime?) -> Unit,
    onRemindWindowEndChanged: (LocalTime?) -> Unit,
    onRepeatIntervalChanged: (String) -> Unit,
    onAddExactReminder: (LocalTime) -> Unit,
    onRemoveExactReminder: (LocalTime) -> Unit,
    onReminderNotificationTitleChanged: (String) -> Unit,
    onReminderNotificationBodyChanged: (String) -> Unit,
    onTargetDurationChanged: (String) -> Unit,
    onAddStep: () -> Unit,
    onStepTitleChanged: (String, String) -> Unit,
    onRemoveStep: (String) -> Unit,
    onDeleteClicked: () -> Unit,
    onShowPicker: (HabitDateTimePickerRequest) -> Unit,
) {
    var showAdvancedReminder by rememberSaveable(uiState.habitId) {
        mutableStateOf(
            uiState.exactReminderTimes.isNotEmpty() ||
                uiState.reminderNotificationTitle.isNotBlank() ||
                uiState.reminderNotificationBody.isNotBlank(),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ZouEditorSection(
            title = "执行目标",
            subtitle = "先决定今天如何完成，再补充提醒细节。",
        ) {
            when (uiState.checkInMode) {
                HabitCheckInMode.CHECK -> {
                    Text(
                        text = "今日命中时可直接打卡。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HabitCheckInMode.STEPS -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "步骤", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        TextButton(modifier = Modifier.testTag("habit_editor_add_step"), onClick = onAddStep) { Text("新增步骤") }
                    }
                    if (uiState.steps.isEmpty()) {
                        Text(
                            text = "还没有步骤。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.steps.forEach { step ->
                                HabitStepEditorRow(
                                    step = step,
                                    onTitleChanged = { onStepTitleChanged(step.id, it) },
                                    onRemoveClicked = { onRemoveStep(step.id) },
                                )
                            }
                        }
                    }
                    uiState.stepsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                HabitCheckInMode.DURATION -> {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.targetDurationText,
                        onValueChange = onTargetDurationChanged,
                        label = { Text("目标时长（分钟）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.targetDurationError != null,
                        supportingText = { uiState.targetDurationError?.let { Text(it) } },
                        colors = noteFlowOutlinedTextFieldColors(),
                    )
                }
            }
        }

        ZouEditorSection(
            title = "提醒策略",
            subtitle = if (showAdvancedReminder) {
                "默认显示常用提醒，特别提醒和通知文案放进高级区。"
            } else {
                uiState.advancedReminderSummary()
            },
        ) {
            StandardFieldRow(label = "提醒窗口开始") {
                ReadOnlyTimeField(label = "开始时间", value = uiState.remindWindowStart?.format(timeFormatter) ?: "未设置")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.testTag("habit_editor_remind_start"),
                        onClick = {
                            onShowPicker(
                                HabitDateTimePickerRequest(
                                    title = "设置开始时间",
                                    mode = ZouDateTimeSheetMode.TIME_ONLY,
                                    initialDateTime = LocalDate.now(zoneId).atTime(uiState.remindWindowStart ?: LocalTime.of(8, 0)),
                                    onConfirm = { selected -> onRemindWindowStartChanged(selected.toLocalTime()) },
                                ),
                            )
                        },
                        colors = noteFlowOutlinedButtonColors(),
                    ) { Text("设置开始时间") }
                    TextButton(onClick = { onRemindWindowStartChanged(null) }, enabled = uiState.remindWindowStart != null) { Text("清除") }
                }
            }

            StandardFieldRow(label = "提醒窗口结束") {
                ReadOnlyTimeField(label = "结束时间", value = uiState.remindWindowEnd?.format(timeFormatter) ?: "未设置")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.testTag("habit_editor_remind_end"),
                        onClick = {
                            onShowPicker(
                                HabitDateTimePickerRequest(
                                    title = "设置结束时间",
                                    mode = ZouDateTimeSheetMode.TIME_ONLY,
                                    initialDateTime = LocalDate.now(zoneId).atTime(uiState.remindWindowEnd ?: LocalTime.of(21, 0)),
                                    onConfirm = { selected -> onRemindWindowEndChanged(selected.toLocalTime()) },
                                ),
                            )
                        },
                        colors = noteFlowOutlinedButtonColors(),
                    ) { Text("设置结束时间") }
                    TextButton(onClick = { onRemindWindowEndChanged(null) }, enabled = uiState.remindWindowEnd != null) { Text("清除") }
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().testTag("habit_editor_repeat_interval"),
                value = uiState.repeatIntervalMinutesText,
                onValueChange = onRepeatIntervalChanged,
                label = { Text("重复提醒间隔（分钟）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = noteFlowOutlinedTextFieldColors(),
            )

            TextButton(onClick = { showAdvancedReminder = !showAdvancedReminder }) {
                Text(if (showAdvancedReminder) "收起高级提醒" else "展开高级提醒")
            }

            if (showAdvancedReminder) {
                StandardFieldRow(label = "特别提醒") {
                    OutlinedButton(
                        modifier = Modifier.testTag("habit_editor_add_exact"),
                        onClick = {
                            val now = LocalDateTime.now(zoneId)
                            onShowPicker(
                                HabitDateTimePickerRequest(
                                    title = "添加特别提醒",
                                    mode = ZouDateTimeSheetMode.TIME_ONLY,
                                    initialDateTime = LocalDate.now(zoneId).atTime(uiState.exactReminderTimes.lastOrNull() ?: now.toLocalTime()),
                                    minimumDateTime = if (uiState.isDueToday(LocalDate.now(zoneId))) now else null,
                                    onConfirm = { selected -> onAddExactReminder(selected.toLocalTime()) },
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
                            uiState.exactReminderTimes.forEach { time ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = time.format(timeFormatter),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    TextButton(onClick = { onRemoveExactReminder(time) }) { Text("删除") }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("habit_editor_notification_title_input"),
                    value = uiState.reminderNotificationTitle,
                    onValueChange = onReminderNotificationTitleChanged,
                    label = { Text("通知标题（可选）") },
                    singleLine = true,
                    colors = noteFlowOutlinedTextFieldColors(),
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("habit_editor_notification_body_input"),
                    value = uiState.reminderNotificationBody,
                    onValueChange = onReminderNotificationBodyChanged,
                    label = { Text("通知正文（可选）") },
                    minLines = 3,
                    maxLines = 4,
                    colors = noteFlowOutlinedTextFieldColors(),
                )
            }

            uiState.reminderError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        if (uiState.canDelete) {
            ZouEditorSection(
                title = "危险操作",
                subtitle = "删除后会进入回收站，不影响底部主动作区。",
            ) {
                TextButton(modifier = Modifier.fillMaxWidth().testTag("habit_editor_delete"), onClick = onDeleteClicked) {
                    Text(text = "软删除习惯", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        uiState.saveErrorMessage?.let {
            Text(text = it, modifier = Modifier.testTag("habit_editor_save_error"), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ReadOnlyTimeField(label: String, value: String) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        colors = noteFlowOutlinedTextFieldColors(),
    )
}

@Composable
private fun HabitStepEditorRow(
    step: HabitStep,
    onTitleChanged: (String) -> Unit,
    onRemoveClicked: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            modifier = Modifier.weight(1f).testTag("habit_step_title_input"),
            value = step.title,
            onValueChange = onTitleChanged,
            label = { Text("步骤标题") },
            singleLine = true,
            colors = noteFlowOutlinedTextFieldColors(),
        )
        TextButton(onClick = onRemoveClicked) { Text("删除") }
    }
}

private fun dayOfWeekLabel(dayOfWeek: DayOfWeek): String =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }

private data class HabitDateTimePickerRequest(
    val title: String,
    val mode: ZouDateTimeSheetMode,
    val initialDateTime: LocalDateTime,
    val minimumDateTime: LocalDateTime? = null,
    val onConfirm: (LocalDateTime) -> Unit,
)

private fun HabitEditorUiState.frequencySummary(): String {
    return when (frequencyType) {
        HabitFrequencyType.DAILY -> "每天执行一次。"
        HabitFrequencyType.WEEKLY -> {
            if (selectedWeekdays.isEmpty()) {
                "按周执行，尚未选择具体星期。"
            } else {
                selectedWeekdays
                    .sortedBy { it.value }
                    .joinToString(prefix = "每周 ", separator = " / ") { dayOfWeekLabel(it) }
            }
        }
        HabitFrequencyType.INTERVAL_DAYS -> {
            val interval = intervalDaysText.toIntOrNull()
            if (interval == null || interval <= 0) {
                "按固定天数间隔执行。"
            } else {
                "每 $interval 天执行一次。"
            }
        }
        HabitFrequencyType.MONTHLY -> {
            if (monthlyDaysText.isBlank()) {
                "按每月日期执行。"
            } else {
                "每月在 $monthlyDaysText 执行。"
            }
        }
    }
}

private fun HabitEditorUiState.advancedReminderSummary(): String {
    val parts = buildList {
        repeatIntervalMinutesText.toIntOrNull()?.takeIf { it > 0 }?.let { add("每 $it 分钟重复") }
        if (exactReminderTimes.isNotEmpty()) add("${exactReminderTimes.size} 个特别提醒")
        if (reminderNotificationTitle.isNotBlank() || reminderNotificationBody.isNotBlank()) add("自定义通知文案")
    }
    return if (parts.isEmpty()) {
        "默认收起特别提醒和通知文案。"
    } else {
        parts.joinToString("，")
    }
}

private fun HabitEditorUiState.isDueToday(today: LocalDate): Boolean {
    return when (frequencyType) {
        HabitFrequencyType.DAILY -> true
        HabitFrequencyType.WEEKLY -> selectedWeekdays.contains(today.dayOfWeek)
        HabitFrequencyType.INTERVAL_DAYS -> {
            val intervalDays = intervalDaysText.toIntOrNull() ?: return false
            val anchorDate = intervalAnchorDate ?: return false
            if (today.isBefore(anchorDate)) return false
            ChronoUnit.DAYS.between(anchorDate, today) % intervalDays == 0L
        }
        HabitFrequencyType.MONTHLY -> {
            monthlyDaysText.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(today.dayOfMonth)
        }
    }
}

private fun Context.shouldRequestNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return false
    }
    return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}
