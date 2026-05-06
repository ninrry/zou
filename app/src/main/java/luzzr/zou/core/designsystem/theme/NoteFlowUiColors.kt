package luzzr.zou.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NoteFlowUiColors(
    val background: Color,
    val backgroundRaised: Color,
    val surface: Color,
    val surfaceFloating: Color,
    val surfaceVariant: Color,
    val canvas: Color,
    val canvasRaised: Color,
    val glassSurface: Color,
    val glassSurfaceStrong: Color,
    val glassInput: Color,
    val overlayAmbient: Color,
    val overlayDeep: Color,
    val glassBorder: Color,
    val glassBorderSoft: Color,
    val glassInnerGlow: Color,
    val glassShadow: Color,
    val outlineSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val onAccent: Color,
)

internal val LightNoteFlowUiColors = NoteFlowUiColors(
    background = NoteFlowBackground,
    backgroundRaised = NoteFlowBackgroundRaised,
    surface = NoteFlowSurface,
    surfaceFloating = NoteFlowSurfaceFloating,
    surfaceVariant = NoteFlowSurfaceVariant,
    canvas = NoteFlowCanvasLayer,
    canvasRaised = NoteFlowCanvasLayerRaised,
    glassSurface = NoteFlowGlassSurface,
    glassSurfaceStrong = NoteFlowGlassSurfaceStrong,
    glassInput = NoteFlowGlassInput,
    overlayAmbient = NoteFlowOverlayAmbient,
    overlayDeep = NoteFlowOverlayDeep,
    glassBorder = NoteFlowGlassBorder,
    glassBorderSoft = NoteFlowGlassBorderSoft,
    glassInnerGlow = NoteFlowGlassInnerGlow,
    glassShadow = NoteFlowGlassShadow,
    outlineSoft = NoteFlowOutlineSoft,
    textPrimary = NoteFlowTextPrimary,
    textSecondary = NoteFlowTextSecondary,
    textTertiary = NoteFlowTextTertiary,
    onAccent = NoteFlowOnAccent,
)

internal val DarkNoteFlowUiColors = NoteFlowUiColors(
    background = Color(0xFF161514),
    backgroundRaised = Color(0xFF1B1917),
    surface = Color(0xFF1D1B19),
    surfaceFloating = Color(0xFF23201D),
    surfaceVariant = Color(0xFF2A2724),
    canvas = Color(0xFF131210),
    canvasRaised = Color(0xFF191714),
    glassSurface = Color(0xCC24211F),
    glassSurfaceStrong = Color(0xE02A2623),
    glassInput = Color(0xD6282421),
    overlayAmbient = Color(0xFFF5EFE5),
    overlayDeep = Color(0xFF0C0A08),
    glassBorder = Color(0x66FFF4E5),
    glassBorderSoft = Color(0x40FFF4E5),
    glassInnerGlow = Color(0x4DFFF8EF),
    glassShadow = Color(0x66000000),
    outlineSoft = Color(0xFF4D4741),
    textPrimary = Color(0xFFF3EFE8),
    textSecondary = Color(0xFFD0C8BD),
    textTertiary = Color(0xFFA79E92),
    onAccent = Color(0xFF171513),
)

internal fun noteFlowUiColors(useDarkTheme: Boolean): NoteFlowUiColors {
    return if (useDarkTheme) DarkNoteFlowUiColors else LightNoteFlowUiColors
}

internal val LocalNoteFlowUiColors = staticCompositionLocalOf { LightNoteFlowUiColors }

object NoteFlowDesignTokens {
    val colors: NoteFlowUiColors
        @Composable get() = LocalNoteFlowUiColors.current
}
