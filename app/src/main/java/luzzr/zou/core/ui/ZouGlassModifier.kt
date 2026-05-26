package luzzr.zou.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import luzzr.zou.core.designsystem.theme.ZouDesignTokens

@Composable
fun Modifier.noteFlowGlassBackground(
    level: GlassLevel = GlassLevel.Normal,
    accentColor: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(24.dp),
): Modifier {
    val designTokens = ZouDesignTokens.colors
    val containerColor = when (level) {
        GlassLevel.Weak -> designTokens.backgroundRaised
        GlassLevel.Normal -> designTokens.surface
        GlassLevel.Strong -> designTokens.surfaceVariant
    }
    val borderColor = when (level) {
        GlassLevel.Weak -> designTokens.glassBorderSoft.copy(alpha = 0.70f)
        GlassLevel.Normal -> designTokens.glassBorderSoft
        GlassLevel.Strong -> designTokens.glassBorder
    }
    val resolvedBorderColor = if (accentColor != Color.Transparent) {
        lerp(
            borderColor,
            accentColor.copy(alpha = borderColor.alpha),
            0.22f,
        )
    } else {
        borderColor
    }

    return this
        .clip(shape)
        .background(containerColor)
        .border(width = 1.dp, color = resolvedBorderColor, shape = shape)
}
