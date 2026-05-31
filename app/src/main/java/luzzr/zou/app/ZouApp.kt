package luzzr.zou.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import luzzr.zou.app.navigation.ZouNavHost
import luzzr.zou.app.navigation.RootRoutes
import luzzr.zou.app.navigation.TopLevelDestination
import luzzr.zou.core.designsystem.theme.MonetColorTokens
import luzzr.zou.core.designsystem.theme.ZouDesignTokens
import luzzr.zou.core.ui.ModuleVisualStyle
import luzzr.zou.core.ui.LocalZouMotion
import luzzr.zou.core.ui.MotionTokens
import luzzr.zou.core.ui.ProvideRadialExpansionController
import luzzr.zou.core.ui.RadialExpansionController
import luzzr.zou.core.ui.RadialExpansionOverlay
import luzzr.zou.core.ui.rememberRadialExpansionController
import luzzr.zou.feature.habits.HabitRoutes
import luzzr.zou.feature.notes.NoteRoutes
import luzzr.zou.feature.settings.SettingsRoutes
import luzzr.zou.feature.tasks.TaskRoutes

@Composable
fun ZouApp(
    pendingTaskDetailId: String? = null,
    pendingHabitDetailId: String? = null,
    onPendingTaskDetailConsumed: () -> Unit = {},
    onPendingHabitDetailConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val designTokens = ZouDesignTokens.colors
    val motion = LocalZouMotion.current
    val navController = rememberNavController()
    val radialExpansionController = rememberRadialExpansionController()
    var selectedTopLevelRoute by rememberSaveable { mutableStateOf(TopLevelDestination.TODAY.route) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { it.route == selectedTopLevelRoute }
        ?: TopLevelDestination.TODAY
    val currentVisualStyle = resolveVisualStyle(currentDestination?.route, currentTopLevel)
    val isTopLevelRoute = currentDestination?.route == RootRoutes.TopLevelCanvas

    val animatedAmbientColor by animateColorAsState(
        targetValue = currentVisualStyle.ambientColor,
        animationSpec = motion.colorShift,
        label = "module_ambient_color",
    )
    val animatedOverlayColor by animateColorAsState(
        targetValue = currentVisualStyle.overlayColor,
        animationSpec = motion.colorShift,
        label = "module_overlay_color",
    )
    val targetOverlayAlpha = if (isTopLevelRoute) 0.004f else 0.010f
    val targetAmbientAlpha = if (isTopLevelRoute) 0.018f else 0.030f
    val targetTopGlowAlpha = if (isTopLevelRoute) 0.040f else 0.140f
    val targetLowerGlowAlpha = if (isTopLevelRoute) 0.010f else 0.055f
    val overlayAlpha by animateFloatAsState(
        targetValue = targetOverlayAlpha,
        animationSpec = motion.tabSwitch,
        label = "module_overlay_alpha",
    )
    val ambientAlpha by animateFloatAsState(
        targetValue = targetAmbientAlpha,
        animationSpec = motion.tabSwitch,
        label = "module_ambient_alpha",
    )
    val topGlowAlpha by animateFloatAsState(
        targetValue = targetTopGlowAlpha,
        animationSpec = motion.tabSwitch,
        label = "module_top_glow_alpha",
    )
    val lowerGlowAlpha by animateFloatAsState(
        targetValue = targetLowerGlowAlpha,
        animationSpec = motion.tabSwitch,
        label = "module_lower_glow_alpha",
    )

    LaunchedEffect(pendingTaskDetailId, pendingHabitDetailId) {
        if (!pendingTaskDetailId.isNullOrBlank()) {
            navController.navigate(TaskRoutes.detailRoute(pendingTaskDetailId)) {
                launchSingleTop = true
            }
            onPendingTaskDetailConsumed()
        }
        if (!pendingHabitDetailId.isNullOrBlank()) {
            navController.navigate(HabitRoutes.detailRoute(pendingHabitDetailId)) {
                launchSingleTop = true
            }
            onPendingHabitDetailConsumed()
        }
    }

    ProvideRadialExpansionController(radialExpansionController) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MonetColorTokens.canvas)
                .drawWithCache {
                    val baseLayer = Brush.verticalGradient(
                        colors = listOf(
                            designTokens.canvasRaised,
                            designTokens.background,
                            designTokens.background,
                        ),
                    )
                    val topGlow = Brush.radialGradient(
                        colors = listOf(
                            animatedAmbientColor.copy(alpha = topGlowAlpha),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.46f, size.height * 0.03f),
                        radius = size.width * 0.74f,
                    )
                    val lowerGlow = Brush.radialGradient(
                        colors = listOf(
                            currentVisualStyle.glassTintColor.copy(alpha = lowerGlowAlpha),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.92f, size.height * 0.88f),
                        radius = size.width * 0.42f,
                    )
                    onDrawBehind {
                        drawRect(baseLayer)
                        if (topGlowAlpha > 0.001f) drawRect(topGlow)
                        if (lowerGlowAlpha > 0.001f) drawRect(lowerGlow)
                        drawRect(animatedOverlayColor.copy(alpha = overlayAlpha))
                        drawRect(designTokens.overlayAmbient.copy(alpha = ambientAlpha))
                    }
                },
        ) {
            ZouNavHost(
                navController = navController,
                selectedTopLevelDestination = currentTopLevel,
                onSelectedTopLevelDestinationChange = { destination ->
                    selectedTopLevelRoute = destination.route
                },
                radialExpansionController = radialExpansionController,
                modifier = Modifier.fillMaxSize(),
            )

            RadialExpansionOverlay(
                controller = radialExpansionController,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun resolveVisualStyle(
    route: String?,
    currentTopLevel: TopLevelDestination,
): ModuleVisualStyle {
    return when {
        route == RootRoutes.TopLevelCanvas || route == null -> currentTopLevel.visualStyle
        route.startsWith(TaskRoutes.listRoute) || route.startsWith("task/") -> TopLevelDestination.TASKS.visualStyle
        route.startsWith(HabitRoutes.listRoute) || route.startsWith("habit/") -> TopLevelDestination.HABITS.visualStyle
        route.startsWith(NoteRoutes.listRoute) || route.startsWith("note/") -> TopLevelDestination.NOTES.visualStyle
        route == SettingsRoutes.settingsRoute || route == SettingsRoutes.trashRoute || route == SettingsRoutes.backupRoute -> {
            TopLevelDestination.TODAY.visualStyle
        }
        else -> TopLevelDestination.TODAY.visualStyle
    }
}
