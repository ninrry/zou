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
        @Composable get() = ZouDesignTokens.colors.canvas
    val canvasRaised: Color
        @Composable get() = ZouDesignTokens.colors.canvasRaised
    val background: Color
        @Composable get() = ZouDesignTokens.colors.background
    val surface: Color
        @Composable get() = ZouDesignTokens.colors.surface
    val surfaceFloating: Color
        @Composable get() = ZouDesignTokens.colors.surfaceFloating
    val textPrimary: Color
        @Composable get() = ZouDesignTokens.colors.textPrimary
    val textSecondary: Color
        @Composable get() = ZouDesignTokens.colors.textSecondary
    val textTertiary: Color
        @Composable get() = ZouDesignTokens.colors.textTertiary

    val today = MonetModulePalette(
        accent = ZouTodayAccent,
        accentSoft = ZouTodayAccentSoft,
        accentGlow = ZouTodayAccentGlow,
    )
    val task = MonetModulePalette(
        accent = ZouTaskAccent,
        accentSoft = ZouTaskAccentSoft,
        accentGlow = ZouTaskAccentGlow,
    )
    val habit = MonetModulePalette(
        accent = ZouHabitAccent,
        accentSoft = ZouHabitAccentSoft,
        accentGlow = ZouHabitAccentGlow,
    )
    val note = MonetModulePalette(
        accent = ZouNoteAccent,
        accentSoft = ZouNoteAccentSoft,
        accentGlow = ZouNoteAccentGlow,
    )
}
