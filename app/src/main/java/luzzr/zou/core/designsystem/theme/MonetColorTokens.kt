package luzzr.zou.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class MonetModulePalette(
    val accent: Color,
    val accentSoft: Color,
    val accentGlow: Color,
)

object MonetColorTokens {
    val canvas: Color
        @Composable get() = NoteFlowDesignTokens.colors.canvas
    val canvasRaised: Color
        @Composable get() = NoteFlowDesignTokens.colors.canvasRaised
    val background: Color
        @Composable get() = NoteFlowDesignTokens.colors.background
    val surface: Color
        @Composable get() = NoteFlowDesignTokens.colors.surface
    val surfaceFloating: Color
        @Composable get() = NoteFlowDesignTokens.colors.surfaceFloating
    val textPrimary: Color
        @Composable get() = NoteFlowDesignTokens.colors.textPrimary
    val textSecondary: Color
        @Composable get() = NoteFlowDesignTokens.colors.textSecondary
    val textTertiary: Color
        @Composable get() = NoteFlowDesignTokens.colors.textTertiary

    val today = MonetModulePalette(
        accent = NoteFlowTodayAccent,
        accentSoft = NoteFlowTodayAccentSoft,
        accentGlow = NoteFlowTodayAccentGlow,
    )
    val task = MonetModulePalette(
        accent = NoteFlowTaskAccent,
        accentSoft = NoteFlowTaskAccentSoft,
        accentGlow = NoteFlowTaskAccentGlow,
    )
    val habit = MonetModulePalette(
        accent = NoteFlowHabitAccent,
        accentSoft = NoteFlowHabitAccentSoft,
        accentGlow = NoteFlowHabitAccentGlow,
    )
    val note = MonetModulePalette(
        accent = NoteFlowNoteAccent,
        accentSoft = NoteFlowNoteAccentSoft,
        accentGlow = NoteFlowNoteAccentGlow,
    )
}
