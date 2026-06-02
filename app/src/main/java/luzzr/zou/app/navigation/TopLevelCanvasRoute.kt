package luzzr.zou.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import luzzr.zou.core.designsystem.theme.ZouDesignTokens
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.designsystem.theme.ZouHabitAccent
import luzzr.zou.core.designsystem.theme.ZouTaskAccentSoft
import luzzr.zou.core.designsystem.theme.ZouHabitAccentSoft
import luzzr.zou.core.designsystem.theme.ZouNoteAccent
import luzzr.zou.core.designsystem.theme.ZouNoteAccentSoft
import luzzr.zou.core.ui.ModuleVisualStyle
import luzzr.zou.core.ui.MotionTokens
import luzzr.zou.core.ui.TopModuleTabBar
import luzzr.zou.core.ui.GlassLevel
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.LocalRadialExpansionController
import luzzr.zou.core.ui.noteFlowPressScale
import luzzr.zou.core.ui.rememberPressInteractionSource
import luzzr.zou.core.ui.LayoutTokens
import luzzr.zou.feature.habits.HabitsRoute
import luzzr.zou.feature.notes.NotesRoute
import luzzr.zou.feature.tasks.TasksRoute
import luzzr.zou.feature.today.TodayRoute
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.min
import androidx.compose.ui.geometry.Offset

internal object RootRoutes {
    const val TopLevelCanvas = "top_level_canvas"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopLevelCanvasRoute(
    selectedDestination: TopLevelDestination,
    onDestinationChanged: (TopLevelDestination) -> Unit,
    onCreateTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onOpenTasks: () -> Unit,
    onCreateHabit: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenHabits: () -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onEditNote: (String) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val designTokens = ZouDesignTokens.colors
    val destinations = TopLevelDestination.entries
    val selectedIndex = destinations.indexOf(selectedDestination).coerceAtLeast(0)
    val latestSelectedDestination by rememberUpdatedState(selectedDestination)
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { destinations.size },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex && pagerState.targetPage != selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .map { page -> destinations[page] }
            .distinctUntilChanged()
            .filter { destination -> destination != latestSelectedDestination }
            .collect { destination ->
                onDestinationChanged(destination)
            }
    }

    val canvasPosition by remember(pagerState, destinations.size) {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (destinations.size - 1).toFloat())
        }
    }
    val lowerIndex = canvasPosition.toInt()
    val upperIndex = min(lowerIndex + 1, destinations.lastIndex)
    val motionFraction = (canvasPosition - lowerIndex).coerceIn(0f, 1f)
    val motionStyle = lerpModuleStyle(
        start = destinations[lowerIndex].visualStyle,
        end = destinations[upperIndex].visualStyle,
        fraction = motionFraction,
    )

    // 🌟 全局快捷创建菜单状态与 FAB 坐标
    var isQuickCreateExpanded by rememberSaveable { mutableStateOf(false) }
    var fabCenter by remember { mutableStateOf<Offset?>(null) }
    val radialExpansionController = LocalRadialExpansionController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .drawWithCache {
                val canvasGlow = Brush.radialGradient(
                    colors = listOf(
                        motionStyle.ambientColor.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.46f, size.height * 0.08f),
                    radius = size.width * 0.68f,
                )
                onDrawBehind {
                    drawRect(canvasGlow)
                    drawRect(motionStyle.overlayColor.copy(alpha = 0.004f))
                    drawRect(designTokens.overlayAmbient.copy(alpha = 0.010f))
                }
            },
    ) {
        // 1. 页面内容层 (HorizontalPager)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = isQuickCreateExpanded,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isQuickCreateExpanded = false // 点击外部任意空白处收起菜单，极致友好
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                // 为每个页面增加 92.dp 底部安全填充，防悬浮 TabBar 遮挡卡片内容
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 92.dp)
                        .graphicsLayer {
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            val normalizedOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
                            val pageAlpha = MotionTokens.CanvasAdjacentAlpha +
                                ((1f - MotionTokens.CanvasAdjacentAlpha) * (1f - normalizedOffset))
                            val pageScale = MotionTokens.CanvasAdjacentScale +
                                ((1f - MotionTokens.CanvasAdjacentScale) * (1f - normalizedOffset))
                            translationX = -size.width * pageOffset * MotionTokens.CanvasParallaxFactor
                            alpha = pageAlpha
                            scaleX = pageScale
                            scaleY = pageScale
                        },
                ) {
                    when (destinations[page]) {
                        TopLevelDestination.TODAY -> TodayRoute(
                            onCreateTask = onCreateTask,
                            onOpenTask = onOpenTask,
                            onEditTask = onEditTask,
                            onOpenTasks = onOpenTasks,
                            onCreateHabit = onCreateHabit,
                            onOpenHabit = onOpenHabit,
                            onEditHabit = onEditHabit,
                            onOpenHabits = onOpenHabits,
                            onOpenSettings = onOpenSettings,
                        )

                        TopLevelDestination.TASKS -> TasksRoute(
                            onCreateTask = onCreateTask,
                            onOpenTask = onOpenTask,
                            onEditTask = onEditTask,
                        )

                        TopLevelDestination.HABITS -> HabitsRoute(
                            onCreateHabit = onCreateHabit,
                            onOpenHabit = onOpenHabit,
                            onEditHabit = onEditHabit,
                        )

                        TopLevelDestination.NOTES -> NotesRoute(
                            onCreateNote = onCreateNote,
                            onOpenNote = onOpenNote,
                            onEditNote = onEditNote,
                        )
                    }
                }
            }
        }

        // 2. FAB 快捷创建二级胶囊级联弹出菜单 (位于 FAB 正上方)
        val isTodayPage = destinations[pagerState.currentPage] == TopLevelDestination.TODAY
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-96).dp) // 位于悬浮栏 74.dp 的上方，留出精致间隙
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StaggeredMenuAction(
                    visible = isQuickCreateExpanded && isTodayPage,
                    delayMillis = 0,
                    text = "新建任务",
                    testTag = "today_quick_create_task",
                    accentColor = ZouTaskAccentSoft,
                    onClick = {
                        isQuickCreateExpanded = false
                        radialExpansionController?.launch(
                            color = ZouTaskAccent,
                            origin = fabCenter, // 精准上报 FAB 绝对中心起爆
                            onNavigate = onCreateTask
                        ) ?: onCreateTask()
                    }
                )
                StaggeredMenuAction(
                    visible = isQuickCreateExpanded && isTodayPage,
                    delayMillis = 80,
                    text = "新建习惯",
                    testTag = "today_quick_create_habit",
                    accentColor = ZouHabitAccentSoft,
                    onClick = {
                        isQuickCreateExpanded = false
                        radialExpansionController?.launch(
                            color = ZouHabitAccent,
                            origin = fabCenter,
                            onNavigate = onCreateHabit
                        ) ?: onCreateHabit()
                    }
                )
                StaggeredMenuAction(
                    visible = isQuickCreateExpanded && isTodayPage,
                    delayMillis = 160,
                    text = "新建笔记",
                    testTag = "today_quick_create_note",
                    accentColor = ZouNoteAccentSoft,
                    onClick = {
                        isQuickCreateExpanded = false
                        radialExpansionController?.launch(
                            color = ZouNoteAccent,
                            origin = fabCenter,
                            onNavigate = onCreateNote
                        ) ?: onCreateNote()
                    }
                )
            }
        }

        // 3. 底部悬浮 TabBar 栏 (居中悬浮)
        TopModuleTabBar(
            destinations = destinations.toList(),
            selectedDestination = destinations[pagerState.currentPage],
            onDestinationSelected = { destination ->
                val targetIndex = destinations.indexOf(destination)
                if (targetIndex != -1) {
                    scope.launch {
                        pagerState.animateScrollToPage(targetIndex)
                    }
                }
            },
            onFabClick = {
                val currentDest = destinations[pagerState.currentPage]
                if (currentDest == TopLevelDestination.TODAY) {
                    isQuickCreateExpanded = !isQuickCreateExpanded
                } else {
                    isQuickCreateExpanded = false
                    when (currentDest) {
                        TopLevelDestination.TASKS -> {
                            radialExpansionController?.launch(
                                color = ZouTaskAccent,
                                origin = fabCenter,
                                onNavigate = onCreateTask
                            ) ?: onCreateTask()
                        }
                        TopLevelDestination.HABITS -> {
                            radialExpansionController?.launch(
                                color = ZouHabitAccent,
                                origin = fabCenter,
                                onNavigate = onCreateHabit
                            ) ?: onCreateHabit()
                        }
                        TopLevelDestination.NOTES -> {
                            radialExpansionController?.launch(
                                color = ZouNoteAccent,
                                origin = fabCenter,
                                onNavigate = onCreateNote
                            ) ?: onCreateNote()
                        }
                        else -> {}
                    }
                }
            },
            fabExpanded = isQuickCreateExpanded && isTodayPage,
            fabPositioned = { center -> fabCenter = center },
            selectionPosition = canvasPosition,
            motionStyle = motionStyle,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun StaggeredMenuAction(
    visible: Boolean,
    delayMillis: Int,
    text: String,
    testTag: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 180f,
            ),
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = 0.76f, // 奶油级奶油温润阻尼，无摆动
                stiffness = 160f,
            ),
            initialOffsetY = { it },
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = 0.76f,
                stiffness = 180f,
            ),
            initialScale = 0.80f,
        ),
        exit = fadeOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 180f,
            ),
        ) + slideOutVertically(
            animationSpec = spring(
                dampingRatio = 0.76f,
                stiffness = 160f,
            ),
            targetOffsetY = { it },
        ) + scaleOut(
            animationSpec = spring(
                dampingRatio = 0.76f,
                stiffness = 180f,
            ),
            targetScale = 0.80f,
        ),
    ) {
        GlassSurface(
            modifier = Modifier
                .noteFlowPressScale(interactionSource = interactionSource, pressedScale = 0.95f)
                .testTag(testTag)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            accentColor = accentColor,
            level = GlassLevel.Normal,
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = LayoutTokens.Space20, vertical = LayoutTokens.Space12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun lerpModuleStyle(
    start: ModuleVisualStyle,
    end: ModuleVisualStyle,
    fraction: Float,
): ModuleVisualStyle {
    return ModuleVisualStyle(
        accentColor = lerp(start.accentColor, end.accentColor, fraction),
        accentSoftColor = lerp(start.accentSoftColor, end.accentSoftColor, fraction),
        accentGlowColor = lerp(start.accentGlowColor, end.accentGlowColor, fraction),
        ambientColor = lerp(start.ambientColor, end.ambientColor, fraction),
        overlayColor = lerp(start.overlayColor, end.overlayColor, fraction),
        glassTintColor = lerp(start.glassTintColor, end.glassTintColor, fraction),
    )
}
