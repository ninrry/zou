package luzzr.zou.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ModuleVisualStyle(
    val accentColor: Color,
    val accentSoftColor: Color,
    val accentGlowColor: Color,
    val ambientColor: Color,
    val overlayColor: Color = accentSoftColor,
    val glassTintColor: Color = accentSoftColor,
)
