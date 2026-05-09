package luzzr.zou.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.ui.graphics.vector.ImageVector
import luzzr.zou.core.designsystem.theme.ZouHabitAccent
import luzzr.zou.core.designsystem.theme.ZouHabitAccentGlow
import luzzr.zou.core.designsystem.theme.ZouHabitAccentSoft
import luzzr.zou.core.designsystem.theme.ZouNoteAccent
import luzzr.zou.core.designsystem.theme.ZouNoteAccentGlow
import luzzr.zou.core.designsystem.theme.ZouNoteAccentSoft
import luzzr.zou.core.designsystem.theme.ZouTaskAccent
import luzzr.zou.core.designsystem.theme.ZouTaskAccentGlow
import luzzr.zou.core.designsystem.theme.ZouTaskAccentSoft
import luzzr.zou.core.designsystem.theme.ZouTodayAccent
import luzzr.zou.core.designsystem.theme.ZouTodayAccentGlow
import luzzr.zou.core.designsystem.theme.ZouTodayAccentSoft
import luzzr.zou.core.ui.ModuleVisualStyle
import luzzr.zou.feature.tasks.TaskRoutes

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val visualStyle: ModuleVisualStyle,
) {
    TODAY(
        route = "today",
        label = "今日",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        visualStyle = ModuleVisualStyle(
            accentColor = ZouTodayAccent,
            accentSoftColor = ZouTodayAccentSoft,
            accentGlowColor = ZouTodayAccentGlow,
            ambientColor = ZouTodayAccentSoft.copy(alpha = 0.72f),
            overlayColor = ZouTodayAccentGlow.copy(alpha = 0.28f),
            glassTintColor = ZouTodayAccentSoft.copy(alpha = 0.68f),
        ),
    ),
    TASKS(
        route = TaskRoutes.listRoute,
        label = "待办",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
        visualStyle = ModuleVisualStyle(
            accentColor = ZouTaskAccent,
            accentSoftColor = ZouTaskAccentSoft,
            accentGlowColor = ZouTaskAccentGlow,
            ambientColor = ZouTaskAccentSoft.copy(alpha = 0.72f),
            overlayColor = ZouTaskAccentGlow.copy(alpha = 0.28f),
            glassTintColor = ZouTaskAccentSoft.copy(alpha = 0.68f),
        ),
    ),
    HABITS(
        route = "habits",
        label = "习惯",
        selectedIcon = Icons.Filled.Loop,
        unselectedIcon = Icons.Outlined.Loop,
        visualStyle = ModuleVisualStyle(
            accentColor = ZouHabitAccent,
            accentSoftColor = ZouHabitAccentSoft,
            accentGlowColor = ZouHabitAccentGlow,
            ambientColor = ZouHabitAccentSoft.copy(alpha = 0.72f),
            overlayColor = ZouHabitAccentGlow.copy(alpha = 0.28f),
            glassTintColor = ZouHabitAccentSoft.copy(alpha = 0.68f),
        ),
    ),
    NOTES(
        route = "notes",
        label = "笔记",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        visualStyle = ModuleVisualStyle(
            accentColor = ZouNoteAccent,
            accentSoftColor = ZouNoteAccentSoft,
            accentGlowColor = ZouNoteAccentGlow,
            ambientColor = ZouNoteAccentSoft.copy(alpha = 0.72f),
            overlayColor = ZouNoteAccentGlow.copy(alpha = 0.28f),
            glassTintColor = ZouNoteAccentSoft.copy(alpha = 0.68f),
        ),
    ),
}
