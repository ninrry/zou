package luzzr.zou.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ZouUiColors(
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
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
)

internal val LightZouUiColors = ZouUiColors(
    background = ZouBackground,
    backgroundRaised = ZouBackgroundRaised,
    surface = ZouSurface,
    surfaceFloating = ZouSurfaceFloating,
    surfaceVariant = ZouSurfaceVariant,
    canvas = ZouCanvasLayer,
    canvasRaised = ZouCanvasLayerRaised,
    glassSurface = ZouGlassSurface,
    glassSurfaceStrong = ZouGlassSurfaceStrong,
    glassInput = ZouGlassInput,
    overlayAmbient = ZouOverlayAmbient,
    overlayDeep = ZouOverlayDeep,
    glassBorder = ZouGlassBorder,
    glassBorderSoft = ZouGlassBorderSoft,
    glassInnerGlow = ZouGlassInnerGlow,
    glassShadow = ZouGlassShadow,
    outlineSoft = ZouOutlineSoft,
    textPrimary = ZouTextPrimary,
    textSecondary = ZouTextSecondary,
    textTertiary = ZouTextTertiary,
    onAccent = ZouOnAccent,
    success = ZouSuccess,
    successSoft = ZouSuccessSoft,
    warning = ZouWarning,
    warningSoft = ZouWarningSoft,
    danger = ZouDanger,
    dangerSoft = ZouDangerSoft,
)

internal val DarkZouUiColors = ZouUiColors(
    background = Color(0xFF171615),
    backgroundRaised = Color(0xFF1D1A18),
    surface = Color(0xFF211E1B),
    surfaceFloating = Color(0xFF27231F),
    surfaceVariant = Color(0xFF302B26),
    canvas = Color(0xFF141312),
    canvasRaised = Color(0xFF1B1815),
    glassSurface = Color(0xD125221E),
    glassSurfaceStrong = Color(0xE62C2722),
    glassInput = Color(0xD62A251F),
    overlayAmbient = Color(0xFFF4ECE0),
    overlayDeep = Color(0xFF0B0907),
    glassBorder = Color(0x70F1E4D1),
    glassBorderSoft = Color(0x45F1E4D1),
    glassInnerGlow = Color(0x52FFF7EA),
    glassShadow = Color(0x66000000),
    outlineSoft = Color(0xFF51493F),
    textPrimary = Color(0xFFF4EEE5),
    textSecondary = Color(0xFFD2C8BA),
    textTertiary = Color(0xFFA99F91),
    onAccent = Color(0xFF171513),
    success = Color(0xFF93C7A4),
    successSoft = Color(0xFF26382D),
    warning = Color(0xFFE2B37A),
    warningSoft = Color(0xFF3F3021),
    danger = Color(0xFFE79A94),
    dangerSoft = Color(0xFF432825),
)

internal fun noteFlowUiColors(useDarkTheme: Boolean): ZouUiColors {
    return if (useDarkTheme) DarkZouUiColors else LightZouUiColors
}

internal val LocalZouUiColors = staticCompositionLocalOf { LightZouUiColors }

object ZouDesignTokens {
    val colors: ZouUiColors
        @Composable get() = LocalZouUiColors.current
}
