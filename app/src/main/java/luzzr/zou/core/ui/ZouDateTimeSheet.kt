package luzzr.zou.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import luzzr.zou.core.designsystem.theme.ZouDesignTokens
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue

enum class ZouDateTimeSheetMode {
    DATE_TIME,
    DATE_ONLY,
    TIME_ONLY,
}

enum class TimePrecisionMode {
    FIVE_MIN,
    ONE_MIN,
}

private enum class DateTimePanel {
    DATE,
    TIME,
}

private object DateTimeSheetText {
    const val Date = "日期"
    const val Time = "时间"
    const val Cancel = "取消"
    const val Confirm = "确认"
    const val Next = "下一步"
    const val Previous = "上一步"
    const val Today = "今天"
    const val Tomorrow = "明天"
    const val DayAfterTomorrow = "后天"
    const val PreviousMonth = "上月"
    const val NextMonth = "下月"
    const val FiveMinutePrecision = "5分钟步进"
    const val OneMinutePrecision = "1分钟精确"
    const val HourSuffix = "时"
    const val MinuteSuffix = "分"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZouDateTimeSheet(
    visible: Boolean,
    title: String,
    mode: ZouDateTimeSheetMode,
    initialDateTime: LocalDateTime,
    minimumDateTime: LocalDateTime? = null,
    accentColor: Color,
    onDismissRequest: () -> Unit,
    onConfirm: (date: LocalDate, time: LocalTime, precisionMode: TimePrecisionMode) -> Unit,
) {
    if (!visible) return

    val designTokens = ZouDesignTokens.colors

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()) }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val nowDate = remember(minimumDateTime) { minimumDateTime?.toLocalDate() ?: LocalDate.now() }
    val effectiveInitialDateTime = remember(title, mode, initialDateTime, minimumDateTime) {
        if (minimumDateTime != null && initialDateTime.isBefore(minimumDateTime)) {
            minimumDateTime
        } else {
            initialDateTime
        }
    }
    val minimumDate = minimumDateTime?.toLocalDate()

    var selectedDate by remember(title, mode, effectiveInitialDateTime.toLocalDate()) {
        mutableStateOf(effectiveInitialDateTime.toLocalDate())
    }
    var selectedHour by remember(title, mode, effectiveInitialDateTime.hour) {
        mutableIntStateOf(effectiveInitialDateTime.hour)
    }
    var selectedMinute by remember(title, mode, effectiveInitialDateTime.minute) {
        mutableIntStateOf(effectiveInitialDateTime.minute)
    }
    var activePanel by remember(title, mode) {
        mutableStateOf(if (mode == ZouDateTimeSheetMode.TIME_ONLY) DateTimePanel.TIME else DateTimePanel.DATE)
    }
    var precisionMode by remember(title, mode) { mutableStateOf(TimePrecisionMode.FIVE_MIN) }
    var visibleMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }

    val minuteValues = remember(precisionMode) {
        when (precisionMode) {
            TimePrecisionMode.FIVE_MIN -> (0..55 step 5).toList()
            TimePrecisionMode.ONE_MIN -> (0..59).toList()
        }
    }

    LaunchedEffect(minuteValues) {
        if (selectedMinute !in minuteValues) {
            selectedMinute = minuteValues.minByOrNull { (it - selectedMinute).absoluteValue } ?: 0
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = designTokens.surfaceFloating.copy(alpha = 0.98f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LayoutTokens.Space20, vertical = LayoutTokens.Space16),
                verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space16),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = when (mode) {
                        ZouDateTimeSheetMode.DATE_ONLY -> selectedDate.format(dateFormatter)
                        ZouDateTimeSheetMode.TIME_ONLY -> LocalTime.of(selectedHour, selectedMinute).format(timeFormatter)
                        ZouDateTimeSheetMode.DATE_TIME -> LocalDateTime.of(
                            selectedDate,
                            LocalTime.of(selectedHour, selectedMinute),
                        ).format(dateTimeFormatter)
                    },
                    modifier = Modifier.testTag("datetime_sheet_preview"),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (mode == ZouDateTimeSheetMode.DATE_TIME) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space8),
                    ) {
                        FilterChip(
                            selected = activePanel == DateTimePanel.DATE,
                            onClick = { activePanel = DateTimePanel.DATE },
                            label = { Text(DateTimeSheetText.Date) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("datetime_panel_date"),
                            colors = noteFlowFilterChipColors(accentColor),
                        )
                        FilterChip(
                            selected = activePanel == DateTimePanel.TIME,
                            onClick = { activePanel = DateTimePanel.TIME },
                            label = { Text(DateTimeSheetText.Time) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("datetime_panel_time"),
                            colors = noteFlowFilterChipColors(accentColor),
                        )
                    }
                }

                val showDatePanel = mode == ZouDateTimeSheetMode.DATE_ONLY ||
                    (mode == ZouDateTimeSheetMode.DATE_TIME && activePanel == DateTimePanel.DATE)
                val showTimePanel = mode == ZouDateTimeSheetMode.TIME_ONLY ||
                    (mode == ZouDateTimeSheetMode.DATE_TIME && activePanel == DateTimePanel.TIME)

                if (showDatePanel) {
                    QuickDateRow(
                        selectedDate = selectedDate,
                        baseDate = nowDate,
                        accentColor = accentColor,
                        onDateSelected = { date ->
                            val clamped = if (minimumDate != null && date < minimumDate) minimumDate else date
                            selectedDate = clamped
                            visibleMonth = YearMonth.from(clamped)
                        },
                    )
                    MonthCalendar(
                        month = visibleMonth,
                        selectedDate = selectedDate,
                        minimumDate = minimumDate,
                        accentColor = accentColor,
                        onMonthChanged = { visibleMonth = it },
                        onDateSelected = { date ->
                            selectedDate = if (minimumDate != null && date < minimumDate) minimumDate else date
                        },
                    )
                }

                if (showTimePanel) {
                    TimePrecisionSelector(
                        precisionMode = precisionMode,
                        accentColor = accentColor,
                        onPrecisionModeChanged = { precisionMode = it },
                    )
                    TimeWheelRow(
                        selectedHour = selectedHour,
                        selectedMinute = selectedMinute,
                        minuteValues = minuteValues,
                        accentColor = accentColor,
                        onHourChanged = { selectedHour = it },
                        onMinuteChanged = { selectedMinute = it },
                    )
                }
            }

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LayoutTokens.Space20, vertical = LayoutTokens.Space12),
                accentColor = accentColor,
                level = GlassLevel.Weak,
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LayoutTokens.Space12),
                    horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space8),
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("datetime_cancel"),
                        onClick = onDismissRequest,
                        colors = noteFlowOutlinedButtonColors(),
                    ) {
                        Text(DateTimeSheetText.Cancel)
                    }
                    if (mode == ZouDateTimeSheetMode.DATE_TIME) {
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .testTag(if (activePanel == DateTimePanel.DATE) "datetime_next" else "datetime_previous"),
                            onClick = {
                                activePanel = if (activePanel == DateTimePanel.DATE) {
                                    DateTimePanel.TIME
                                } else {
                                    DateTimePanel.DATE
                                }
                            },
                            colors = noteFlowOutlinedButtonColors(),
                        ) {
                            Text(
                                if (activePanel == DateTimePanel.DATE) {
                                    DateTimeSheetText.Next
                                } else {
                                    DateTimeSheetText.Previous
                                },
                            )
                        }
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("datetime_confirm"),
                        onClick = {
                            val confirmed = clampConfirmedDateTime(
                                mode = mode,
                                selectedDate = selectedDate,
                                selectedHour = selectedHour,
                                selectedMinute = selectedMinute,
                                minimumDateTime = minimumDateTime,
                                baseDate = effectiveInitialDateTime.toLocalDate(),
                            )
                            onConfirm(confirmed.toLocalDate(), confirmed.toLocalTime(), precisionMode)
                        },
                        colors = noteFlowButtonColors(accentColor),
                    ) {
                        Text(DateTimeSheetText.Confirm)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDateRow(
    selectedDate: LocalDate,
    baseDate: LocalDate,
    accentColor: Color,
    onDateSelected: (LocalDate) -> Unit,
) {
    val candidates = listOf(
        DateTimeSheetText.Today to baseDate,
        DateTimeSheetText.Tomorrow to baseDate.plusDays(1),
        DateTimeSheetText.DayAfterTomorrow to baseDate.plusDays(2),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space8),
    ) {
        candidates.forEach { (label, date) ->
            FilterChip(
                selected = selectedDate == date,
                onClick = { onDateSelected(date) },
                label = { Text(label) },
                modifier = Modifier.testTag(
                    when (label) {
                        DateTimeSheetText.Today -> "datetime_quick_today"
                        DateTimeSheetText.Tomorrow -> "datetime_quick_tomorrow"
                        else -> "datetime_quick_day_after"
                    },
                ),
                colors = noteFlowFilterChipColors(accentColor),
                leadingIcon = if (selectedDate == date) {
                    {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    minimumDate: LocalDate?,
    accentColor: Color,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val designTokens = ZouDesignTokens.colors
    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM", Locale.getDefault()) }
    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()
    val totalCellCount = ((firstDayOffset + dayCount + 6) / 7) * 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .noteFlowGlassBackground(
                level = GlassLevel.Weak,
                accentColor = accentColor,
                shape = MaterialTheme.shapes.large,
            )
            .padding(LayoutTokens.Space12),
        verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onMonthChanged(month.minusMonths(1)) }) {
                Text(DateTimeSheetText.PreviousMonth)
            }
            Text(
                text = month.atDay(1).format(monthFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = { onMonthChanged(month.plusMonths(1)) }) {
                Text(DateTimeSheetText.NextMonth)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = designTokens.textSecondary,
                    )
                }
            }
        }

        for (rowIndex in 0 until totalCellCount / 7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space8),
            ) {
                for (columnIndex in 0 until 7) {
                    val cellIndex = rowIndex * 7 + columnIndex
                    val day = cellIndex - firstDayOffset + 1
                    if (day !in 1..dayCount) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                        )
                    } else {
                        val cellDate = month.atDay(day)
                        val selected = cellDate == selectedDate
                        val enabled = minimumDate == null || !cellDate.isBefore(minimumDate)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(if (selected) accentColor.copy(alpha = 0.24f) else Color.Transparent)
                                .clickable(
                                    interactionSource = MutableInteractionSource(),
                                    enabled = enabled,
                                ) { onDateSelected(cellDate) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                                    selected -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun clampConfirmedDateTime(
    mode: ZouDateTimeSheetMode,
    selectedDate: LocalDate,
    selectedHour: Int,
    selectedMinute: Int,
    minimumDateTime: LocalDateTime?,
    baseDate: LocalDate,
): LocalDateTime {
    val candidate = when (mode) {
        ZouDateTimeSheetMode.DATE_ONLY -> selectedDate.atStartOfDay()
        ZouDateTimeSheetMode.TIME_ONLY -> LocalDateTime.of(baseDate, LocalTime.of(selectedHour, selectedMinute))
        ZouDateTimeSheetMode.DATE_TIME -> LocalDateTime.of(selectedDate, LocalTime.of(selectedHour, selectedMinute))
    }
    return if (minimumDateTime != null && candidate.isBefore(minimumDateTime)) minimumDateTime else candidate
}

@Composable
private fun TimePrecisionSelector(
    precisionMode: TimePrecisionMode,
    accentColor: Color,
    onPrecisionModeChanged: (TimePrecisionMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space8),
    ) {
        FilterChip(
            selected = precisionMode == TimePrecisionMode.FIVE_MIN,
            onClick = { onPrecisionModeChanged(TimePrecisionMode.FIVE_MIN) },
            label = { Text(DateTimeSheetText.FiveMinutePrecision) },
            modifier = Modifier.testTag("datetime_precision_five"),
            colors = noteFlowFilterChipColors(accentColor),
            leadingIcon = if (precisionMode == TimePrecisionMode.FIVE_MIN) {
                {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                    )
                }
            } else {
                null
            },
        )
        FilterChip(
            selected = precisionMode == TimePrecisionMode.ONE_MIN,
            onClick = { onPrecisionModeChanged(TimePrecisionMode.ONE_MIN) },
            label = { Text(DateTimeSheetText.OneMinutePrecision) },
            modifier = Modifier.testTag("datetime_precision_one"),
            colors = noteFlowFilterChipColors(accentColor),
            leadingIcon = if (precisionMode == TimePrecisionMode.ONE_MIN) {
                {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                    )
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun TimeWheelRow(
    selectedHour: Int,
    selectedMinute: Int,
    minuteValues: List<Int>,
    accentColor: Color,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelPicker(
            values = (0..23).toList(),
            selectedValue = selectedHour,
            valueFormatter = { value -> "%02d ${DateTimeSheetText.HourSuffix}".format(value) },
            accentColor = accentColor,
            modifier = Modifier
                .weight(1f)
                .testTag("datetime_hour_wheel"),
            onValueChanged = onHourChanged,
        )
        WheelPicker(
            values = minuteValues,
            selectedValue = selectedMinute,
            valueFormatter = { value -> "%02d ${DateTimeSheetText.MinuteSuffix}".format(value) },
            accentColor = accentColor,
            modifier = Modifier
                .weight(1f)
                .testTag("datetime_minute_wheel"),
            onValueChanged = onMinuteChanged,
        )
    }
}

@Composable
private fun WheelPicker(
    values: List<Int>,
    selectedValue: Int,
    valueFormatter: (Int) -> String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onValueChanged: (Int) -> Unit,
) {
    val itemHeight = 40.dp
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(values, selectedIndex) {
        listState.scrollToItem(selectedIndex)
    }

    LaunchedEffect(listState, values, selectedValue) {
        snapshotFlow {
            val firstIndex = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val rounded = if (offset > 20) firstIndex + 1 else firstIndex
            rounded.coerceIn(0, values.lastIndex)
        }
            .map { values[it] }
            .distinctUntilChanged()
            .collect { value ->
                if (value != selectedValue) {
                    onValueChanged(value)
                }
            }
    }

    Box(
        modifier = modifier
            .height(itemHeight * 5)
            .noteFlowGlassBackground(
                level = GlassLevel.Weak,
                shape = MaterialTheme.shapes.large,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = LayoutTokens.Space12)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor.copy(alpha = 0.18f))
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.40f),
                    shape = RoundedCornerShape(16.dp),
                ),
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * 2),
        ) {
            itemsIndexed(values) { _, value ->
                val selected = value == selectedValue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                        ) { onValueChanged(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = valueFormatter(value),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

