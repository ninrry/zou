package luzzr.zou.core.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import luzzr.zou.app.navigation.TopLevelDestination
import luzzr.zou.core.designsystem.theme.ZouDesignTokens
import luzzr.zou.core.designsystem.theme.MonetColorTokens
import kotlin.math.abs

import androidx.compose.animation.animateColorAsState

@Composable
fun TopModuleTabBar(
    destinations: List<TopLevelDestination>,
    selectedDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    onFabClick: () -> Unit = {},
    fabExpanded: Boolean = false,
    fabPositioned: (Offset) -> Unit = {},
    selectionPosition: Float = destinations.indexOf(selectedDestination).coerceAtLeast(0).toFloat(),
    motionStyle: ModuleVisualStyle = selectedDestination.visualStyle,
    modifier: Modifier = Modifier,
) {
    val motion = LocalZouMotion.current
    val designTokens = ZouDesignTokens.colors
    val animatedAccentColor by animateColorAsState(
        targetValue = motionStyle.accentColor,
        animationSpec = motion.colorShift,
        label = "tab_accent_color"
    )
    val animatedAccentGlowColor by animateColorAsState(
        targetValue = motionStyle.accentGlowColor,
        animationSpec = motion.colorShift,
        label = "tab_accent_glow_color"
    )
    val animatedGlassTintColor by animateColorAsState(
        targetValue = motionStyle.glassTintColor,
        animationSpec = motion.colorShift,
        label = "tab_glass_tint_color"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_level_${selectedDestination.route}")
            .navigationBarsPadding() // 强力阻绝贴边，腾出底部安全区
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        val totalSlotCount = 5
        val normalizedSelectionPosition = selectionPosition.coerceIn(0f, 3f)

        // 🌟 核心动效：滑轨跨越映射分段函数，在 Pager 滑过中间槽位（FAB）时跳跃并以 2 倍速度流畅跨越，杜绝重叠与突兀
        val slotPosition = if (normalizedSelectionPosition < 1.0f) {
            normalizedSelectionPosition
        } else if (normalizedSelectionPosition < 2.0f) {
            2f * normalizedSelectionPosition - 1f
        } else {
            normalizedSelectionPosition + 1f
        }

        val slotWidth = maxWidth / totalSlotCount
        val highlightWidth = slotWidth - 12.dp
        val highlightOffset = slotWidth * slotPosition + 6.dp
        val animatedHighlightOffset by animateDpAsState(
            targetValue = highlightOffset,
            animationSpec = motion.tabSwitchDp,
            label = "tab_highlight_offset",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(designTokens.glassSurface.copy(alpha = 0.56f))
                .drawWithCache {
                    val overlay = Brush.verticalGradient(
                        colors = listOf(
                            designTokens.glassInnerGlow.copy(alpha = 0.30f),
                            animatedGlassTintColor.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    )
                    onDrawBehind {
                        drawRoundRect(
                            brush = overlay,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(64f, 64f),
                        )
                        drawRoundRect(
                            color = designTokens.glassBorder.copy(alpha = 0.36f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(64f, 64f),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                },
        ) {
            // 滑动指示器
            TopTabHighlight(
                modifier = Modifier
                    .offset(x = animatedHighlightOffset, y = 8.dp)
                    .height(58.dp),
                width = highlightWidth,
                accentColor = animatedAccentColor,
                accentGlowColor = animatedAccentGlowColor,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. 左侧两个模块 (今日, 待办)
                destinations.take(2).forEachIndexed { index, destination ->
                    val selectionProgress = (1f - abs(slotPosition - index)).coerceIn(0f, 1f)
                    TabItem(
                        destination = destination,
                        selectionProgress = selectionProgress,
                        onSelected = { onDestinationSelected(destination) }
                    )
                }

                // 2. 中间大 FAB 槽位 (索引 2)
                val fabInteraction = rememberPressInteractionSource()
                val fabRotation by animateFloatAsState(
                    targetValue = if (fabExpanded) 45f else 0f,
                    animationSpec = tween(280, easing = MotionTokens.EasingEmphasized),
                    label = "bottom_fab_rotation"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val monetPalette = when (selectedDestination) {
                        TopLevelDestination.TODAY -> MonetColorTokens.today
                        TopLevelDestination.TASKS -> MonetColorTokens.task
                        TopLevelDestination.HABITS -> MonetColorTokens.habit
                        TopLevelDestination.NOTES -> MonetColorTokens.note
                    }

                    val animatedFabAccentColor by animateColorAsState(
                        targetValue = monetPalette.accent,
                        animationSpec = tween(350, easing = MotionTokens.EasingEmphasized),
                        label = "fab_monet_accent"
                    )
                    val animatedFabSoftColor by animateColorAsState(
                        targetValue = monetPalette.accentSoft,
                        animationSpec = tween(350, easing = MotionTokens.EasingEmphasized),
                        label = "fab_monet_soft"
                    )

                    GlassSurface(
                        modifier = Modifier
                            .offset(y = 0.dp) // 原生对准扁平嵌入，去除夸张偏置
                            .size(58.dp)
                            .noteFlowPressScale(interactionSource = fabInteraction, pressedScale = 0.92f)
                            .onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInWindow()
                                val size = coordinates.size
                                if (size.width > 0 && size.height > 0 && position.x > 0) {
                                    fabPositioned(
                                        Offset(
                                            x = position.x + size.width / 2f,
                                            y = position.y + size.height / 2f
                                        )
                                    )
                                }
                            }
                            .clickable(
                                interactionSource = fabInteraction,
                                indication = null,
                                onClick = onFabClick
                            )
                            .testTag("top_level_create_fab"),
                        accentColor = animatedFabAccentColor,
                        level = GlassLevel.Strong,
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(animatedFabSoftColor.copy(alpha = 0.42f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (fabExpanded) "收起快速创建" else "展开快速创建",
                                tint = animatedFabAccentColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        rotationZ = fabRotation
                                    }
                            )
                        }
                    }
                }

                // 3. 右侧两个模块 (习惯, 笔记)
                destinations.takeLast(2).forEachIndexed { index, destination ->
                    val actualSlotIndex = index + 3
                    val selectionProgress = (1f - abs(slotPosition - actualSlotIndex)).coerceIn(0f, 1f)
                    TabItem(
                        destination = destination,
                        selectionProgress = selectionProgress,
                        onSelected = { onDestinationSelected(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    destination: TopLevelDestination,
    selectionProgress: Float,
    onSelected: () -> Unit
) {
    val designTokens = ZouDesignTokens.colors
    val interactionSource = rememberPressInteractionSource()
    Box(
        modifier = Modifier
            .weight(1f)
            .height(74.dp)
            .noteFlowPressScale(interactionSource = interactionSource)
            .graphicsLayer {
                val scale = 0.96f + (selectionProgress * 0.04f)
                scaleX = scale
                scaleY = scale
                translationY = (-2.dp.toPx()) * selectionProgress
            }
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelected
            )
            .testTag("nav_${destination.route}"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (selectionProgress > 0.55f) {
                    destination.selectedIcon
                } else {
                    destination.unselectedIcon
                },
                contentDescription = destination.label,
                tint = lerp(
                    start = designTokens.textSecondary,
                    stop = MaterialTheme.colorScheme.onPrimary,
                    fraction = selectionProgress,
                ),
            )
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelLarge,
                color = lerp(
                    start = designTokens.textTertiary,
                    stop = MaterialTheme.colorScheme.onPrimary,
                    fraction = selectionProgress,
                ),
            )
        }
    }
}

@Composable
private fun TopTabHighlight(
    width: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    accentGlowColor: Color,
    modifier: Modifier = Modifier,
) {
    val designTokens = ZouDesignTokens.colors
    Box(
        modifier = modifier
            .width(width)
            .drawWithCache {
                val topGlow = Brush.verticalGradient(
                    colors = listOf(
                        designTokens.glassInnerGlow.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                )
                val coreGlow = Brush.radialGradient(
                    colors = listOf(
                        designTokens.glassInnerGlow.copy(alpha = 0.18f),
                        accentGlowColor.copy(alpha = 0.24f),
                        Color.Transparent,
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.52f, size.height * 0.38f),
                    radius = size.width * 0.56f,
                )
                val bodyGradient = Brush.verticalGradient(
                    colors = listOf(
                        accentGlowColor.copy(alpha = 0.72f),
                        accentColor.copy(alpha = 0.88f),
                    ),
                )
                onDrawBehind {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.20f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                    )
                    drawRoundRect(
                        brush = bodyGradient,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                    )
                    drawRoundRect(
                        brush = coreGlow,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                    )
                    drawRoundRect(
                        brush = topGlow,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                    )
                    drawRoundRect(
                        color = designTokens.glassInnerGlow.copy(alpha = 0.14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
            .clip(RoundedCornerShape(28.dp)),
    )
}
