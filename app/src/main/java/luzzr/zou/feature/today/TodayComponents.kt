package luzzr.zou.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import luzzr.zou.core.designsystem.theme.ZouDesignTokens
import luzzr.zou.core.designsystem.theme.ZouHabitAccent
import luzzr.zou.core.designsystem.theme.ZouHabitAccentSoft
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.designsystem.theme.ZouTaskAccentSoft
import luzzr.zou.core.designsystem.theme.ZouTodayAccent
import luzzr.zou.core.designsystem.theme.ZouTodayAccentSoft
import luzzr.zou.core.ui.GlassLevel
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.LocalRadialExpansionController
import luzzr.zou.core.ui.ModuleFab
import luzzr.zou.core.ui.MotionTokens
import luzzr.zou.core.ui.noteFlowButtonColors
import luzzr.zou.core.ui.LayoutTokens
import luzzr.zou.core.ui.noteFlowOutlinedButtonColors
import luzzr.zou.core.ui.noteFlowPressScale
import luzzr.zou.core.ui.rememberPressInteractionSource
import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.TaskQuickActionType
import luzzr.zou.feature.settings.TopLevelSettingsButton

internal data class TodayCompactLayoutSpec(
    val columnGap: Dp,
    val sectionGap: Dp,
    val cardGap: Dp,
    val cardPadding: Dp,
    val cardMinHeight: Dp,
    val emptyCardMinHeight: Dp,
    val controlHeight: Dp,
    val actionWidth: Dp,
    val sectionTitleStyle: TextStyle,
    val cardTitleStyle: TextStyle,
    val actionStyle: TextStyle,
    val supportStyle: TextStyle,
)

@Composable
internal fun rememberTodayCompactLayoutSpec(totalWidth: Dp): TodayCompactLayoutSpec {
    val typography = MaterialTheme.typography
    val dense = totalWidth < 360.dp
    return if (dense) {
        TodayCompactLayoutSpec(
            columnGap = 10.dp,
            sectionGap = 8.dp,
            cardGap = LayoutTokens.Space8,
            cardPadding = 12.dp,
            cardMinHeight = 110.dp,
            emptyCardMinHeight = 160.dp,
            controlHeight = 30.dp,
            actionWidth = 62.dp,
            sectionTitleStyle = typography.titleSmall,
            cardTitleStyle = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            actionStyle = typography.labelSmall,
            supportStyle = typography.bodySmall,
        )
    } else {
        TodayCompactLayoutSpec(
            columnGap = 12.dp,
            sectionGap = LayoutTokens.Space12,
            cardGap = LayoutTokens.Space12,
            cardPadding = LayoutTokens.ScreenHorizontalPadding,
            cardMinHeight = 118.dp,
            emptyCardMinHeight = 176.dp,
            controlHeight = 34.dp,
            actionWidth = 72.dp,
            sectionTitleStyle = typography.titleMedium,
            cardTitleStyle = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            actionStyle = typography.labelMedium,
            supportStyle = typography.bodySmall,
        )
    }
}

@Composable
fun TodayHeroCard(
    title: String,
    dateLine: String,
    summary: TodaySummaryUiModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_summary_card"),
        accentColor = ZouTodayAccent,
        level = GlassLevel.Normal,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LayoutTokens.ScreenHorizontalPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$dateLine · ${todaySummaryHeadline(summary)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TopLevelSettingsButton(onClick = onOpenSettings)
            }
            TodayStatusPills(summary = summary)
            TodayCompletionBar(summary = summary)
        }
    }
}

@Composable
internal fun TodaySectionHeader(
    title: String,
    count: Int,
    actionLabel: String,
    testTag: String,
    layoutSpec: TodayCompactLayoutSpec,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = layoutSpec.sectionTitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TodaySectionCountPill(count = count)
        }
        TextButton(
            modifier = Modifier
                .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp)
                .testTag(testTag),
            onClick = onActionClick,
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun TodayTaskCard(
    item: TodayTaskCardUiModel,
    layoutSpec: TodayCompactLayoutSpec,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: () -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    // 动态色彩流转：当任务勾选完成时，晕染主色在 320ms 内渐变为低饱和烟灰色，未完成则呈任务亮色
    val currentAccentColor = if (item.isCompleted) {
        ZouDesignTokens.colors.textTertiary
    } else {
        ZouTaskAccent
    }
    TodayQuickCard(
        modifier = Modifier
            .testTag("today_task_${item.id}")
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        title = item.title,
        subtitle = item.remainingTimeText,
        subtitleColor = MaterialTheme.colorScheme.error,
        accentColor = currentAccentColor,
        layoutSpec = layoutSpec,
        action = {
            TodayQuickActionButton(
                text = item.actionLabel,
                accentColor = currentAccentColor,
                height = layoutSpec.controlHeight,
                width = layoutSpec.actionWidth,
                textStyle = layoutSpec.actionStyle,
                enabled = item.actionEnabled && item.actionType != TaskQuickActionType.NONE,
                testTag = "today_task_action_${item.id}",
                onClick = onAction,
            )
        },
    )
}

@Composable
internal fun TodayHabitCard(
    item: TodayHabitCardUiModel,
    layoutSpec: TodayCompactLayoutSpec,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    // 动态色彩流转：习惯打卡完成时色温流转至沉静灰色，未打卡则为生机勃勃的习惯青绿色
    val currentAccentColor = if (item.isCompleted) {
        ZouDesignTokens.colors.textTertiary
    } else {
        ZouHabitAccent
    }
    TodayQuickCard(
        modifier = Modifier
            .testTag("today_habit_${item.id}")
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        title = item.title,
        subtitle = item.statusHint,
        support = item.progressText,
        accentColor = currentAccentColor,
        layoutSpec = layoutSpec,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TodayQuickActionButton(
                    text = item.primaryActionLabel,
                    accentColor = currentAccentColor,
                    height = layoutSpec.controlHeight,
                    width = layoutSpec.actionWidth,
                    textStyle = layoutSpec.actionStyle,
                    enabled = item.primaryActionEnabled && item.primaryActionType != HabitQuickActionType.NONE,
                    testTag = "today_habit_primary_action_${item.id}",
                    onClick = onPrimaryAction,
                )
                if (!item.secondaryActionLabel.isNullOrBlank()) {
                    TodayQuickActionButton(
                        text = item.secondaryActionLabel,
                        accentColor = if (item.isCompleted) ZouDesignTokens.colors.textTertiary.copy(alpha = 0.7f) else ZouHabitAccentSoft,
                        height = layoutSpec.controlHeight,
                        width = layoutSpec.actionWidth,
                        textStyle = layoutSpec.actionStyle,
                        enabled = item.secondaryActionEnabled && item.secondaryActionType != null,
                        testTag = "today_habit_secondary_action_${item.id}",
                        onClick = onSecondaryAction,
                    )
                }
            }
        },
    )
}

@Composable
private fun TodayQuickCard(
    modifier: Modifier,
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    support: String? = null,
    accentColor: Color,
    layoutSpec: TodayCompactLayoutSpec,
    action: @Composable RowScope.() -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .noteFlowPressScale(interactionSource = interactionSource),
        accentColor = accentColor,
        level = GlassLevel.Normal,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = layoutSpec.cardMinHeight)
                .padding(horizontal = layoutSpec.cardPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TodayCardLeadingAccent(accentColor = accentColor)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = layoutSpec.cardTitleStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = layoutSpec.supportStyle,
                            color = subtitleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!support.isNullOrBlank()) {
                        Text(
                            text = support,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = action,
            )
        }
    }
}

@Composable
private fun TodayCardLeadingAccent(
    accentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(999.dp),
) {
    Box(
        modifier = modifier
            .background(accentColor.copy(alpha = 0.12f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .background(accentColor, CircleShape)
                .widthIn(min = 6.dp)
                .height(6.dp),
        )
    }
}

@Composable
private fun TodayQuickActionButton(
    text: String,
    accentColor: Color,
    height: Dp,
    width: Dp,
    textStyle: TextStyle,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .height(height)
            .testTag(testTag),
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(height / 2),
        colors = noteFlowButtonColors(accentColor),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.widthIn(min = width - 20.dp),
            style = textStyle,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun TodayEmptySectionCard(
    title: String,
    description: String,
    accentColor: Color,
    layoutSpec: TodayCompactLayoutSpec,
    testTag: String,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = layoutSpec.emptyCardMinHeight)
            .testTag(testTag),
        accentColor = accentColor,
        level = GlassLevel.Normal,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(layoutSpec.cardPadding),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TodayCardLeadingAccent(accentColor = accentColor)
            Text(
                text = title,
                style = layoutSpec.cardTitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = layoutSpec.supportStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TodayGlobalEmptyCard(
    onCreateTask: () -> Unit,
    onCreateHabit: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_empty_state"),
        accentColor = ZouTodayAccent,
        level = GlassLevel.Normal,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LayoutTokens.ScreenHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "今天还没有需要处理的内容",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "先创建任务或习惯。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space12)) {
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("today_empty_create_task"),
                    onClick = onCreateTask,
                    colors = noteFlowOutlinedButtonColors(),
                ) {
                    Text("新建任务", maxLines = 1)
                }
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("today_empty_create_habit"),
                    onClick = onCreateHabit,
                    colors = noteFlowOutlinedButtonColors(),
                ) {
                    Text("新建习惯", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TodayStatusPills(summary: TodaySummaryUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_summary_grid"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TodayStatusPill(
            modifier = Modifier.weight(1f),
            label = "待办",
            value = summary.pendingTaskCount.toString(),
            accentColor = ZouTaskAccent,
        )
        TodayStatusPill(
            modifier = Modifier.weight(1f),
            label = "习惯",
            value = summary.dueHabitCount.toString(),
            accentColor = ZouHabitAccent,
        )
        TodayStatusPill(
            modifier = Modifier.weight(1f),
            label = "完成",
            value = summary.completedCount.toString(),
            accentColor = ZouTodayAccent,
        )
    }
}

@Composable
private fun TodayStatusPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val designTokens = ZouDesignTokens.colors
    GlassSurface(
        modifier = modifier,
        accentColor = accentColor,
        level = GlassLevel.Weak,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.14f), CircleShape)
                    .padding(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(accentColor, CircleShape)
                        .padding(3.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = designTokens.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TodayCompletionBar(summary: TodaySummaryUiModel) {
    val designTokens = ZouDesignTokens.colors
    val progress = remember(summary) {
        val total = summary.pendingTaskCount + summary.dueHabitCount + summary.completedCount
        if (total == 0) 0f else summary.completedCount / total.toFloat()
    }
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress.coerceAtLeast(0.06f),
        animationSpec = MotionTokens.SpringSmooth,
        label = "today_progress_bar",
    )
    val completedPercent = (clampedProgress * 100).toInt()
    val remainingItems = todayActiveItemCount(summary)
    val hasItems = remainingItems > 0 || summary.completedCount > 0
    val showFill = clampedProgress > 0f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (hasItems) "完成 $completedPercent%" else "进度",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (remainingItems == 0 && summary.completedCount > 0) "当前已清空"
                       else if (!hasItems) "暂无待办"
                       else "剩余 $remainingItems 项",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasItems || clampedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        color = ZouTodayAccent.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(999.dp),
                    ),
            ) {
                if (showFill) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(10.dp)
                            .background(
                                color = ZouTodayAccent,
                                shape = RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaySectionCountPill(count: Int) {
    val designTokens = ZouDesignTokens.colors
    Box(
        modifier = Modifier
            .background(
                color = designTokens.outlineSoft.copy(alpha = 0.64f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = designTokens.textSecondary,
        )
    }
}

private fun todaySummaryHeadline(summary: TodaySummaryUiModel): String {
    val activeItems = todayActiveItemCount(summary)
    return when {
        activeItems == 0 && summary.completedCount == 0 -> "今日暂无安排"
        activeItems == 0 -> "今日关键项已处理完"
        activeItems == 1 -> "还有 1 项待推进"
        else -> "还有 $activeItems 项待推进"
    }
}

private fun todayActiveItemCount(summary: TodaySummaryUiModel): Int {
    return summary.pendingTaskCount + summary.dueHabitCount
}

