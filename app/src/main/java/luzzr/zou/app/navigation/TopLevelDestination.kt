package luzzr.zou.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import luzzr.zou.core.designsystem.theme.NoteFlowHabitAccent
import luzzr.zou.core.designsystem.theme.NoteFlowHabitAccentGlow
import luzzr.zou.core.designsystem.theme.NoteFlowHabitAccentSoft
import luzzr.zou.core.designsystem.theme.NoteFlowNoteAccent
import luzzr.zou.core.designsystem.theme.NoteFlowNoteAccentGlow
import luzzr.zou.core.designsystem.theme.NoteFlowNoteAccentSoft
import luzzr.zou.core.designsystem.theme.NoteFlowTaskAccent
import luzzr.zou.core.designsystem.theme.NoteFlowTaskAccentGlow
import luzzr.zou.core.designsystem.theme.NoteFlowTaskAccentSoft
import luzzr.zou.core.designsystem.theme.NoteFlowTodayAccent
import luzzr.zou.core.designsystem.theme.NoteFlowTodayAccentGlow
import luzzr.zou.core.designsystem.theme.NoteFlowTodayAccentSoft
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
            accentColor = NoteFlowTodayAccent,
            accentSoftColor = NoteFlowTodayAccentSoft,
            accentGlowColor = NoteFlowTodayAccentGlow,
            ambientColor = NoteFlowTodayAccentSoft.copy(alpha = 0.72f),
            overlayColor = NoteFlowTodayAccentGlow.copy(alpha = 0.28f),
            glassTintColor = NoteFlowTodayAccentSoft.copy(alpha = 0.68f),
        ),
    ),
    TASKS(
        route = TaskRoutes.listRoute,
        label = "待办",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
        visualStyle = ModuleVisualStyle(
            accentColor = NoteFlowTaskAccent,
            accentSoftColor = NoteFlowTaskAccentSoft,
            accentGlowColor = NoteFlowTaskAccentGlow,
            ambientColor = NoteFlowTaskAccentSoft.copy(alpha = 0.72f),
            overlayColor = NoteFlowTaskAccentGlow.copy(alpha = 0.28f),
            glassTintColor = NoteFlowTaskAccentSoft.copy(alpha = 0.68f),
        ),
    ),
    HABITS(
        route = "habits",
        label = "习惯",
        selectedIcon = Icons.Filled.Refresh,
        unselectedIcon = Icons.Outlined.Refresh,
        visualStyle = ModuleVisualStyle(
            accentColor = NoteFlowHabitAccent,
            accentSoftColor = NoteFlowHabitAccentSoft,
            accentGlowColor = NoteFlowHabitAccentGlow,
            ambientColor = NoteFlowHabitAccentSoft.copy(alpha = 0.72f),
            overlayColor = NoteFlowHabitAccentGlow.copy(alpha = 0.28f),
            glassTintColor = NoteFlowHabitAccentSoft.copy(alpha = 0.68f),
        ),
    ),
    NOTES(
        route = "notes",
        label = "笔记",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        visualStyle = ModuleVisualStyle(
            accentColor = NoteFlowNoteAccent,
            accentSoftColor = NoteFlowNoteAccentSoft,
            accentGlowColor = NoteFlowNoteAccentGlow,
            ambientColor = NoteFlowNoteAccentSoft.copy(alpha = 0.72f),
            overlayColor = NoteFlowNoteAccentGlow.copy(alpha = 0.28f),
            glassTintColor = NoteFlowNoteAccentSoft.copy(alpha = 0.68f),
        ),
    ),
}
