package luzzr.zou.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import luzzr.zou.core.designsystem.theme.ZouDesignTokens
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.lerp

enum class GlassLevel {
    Weak,
    Normal,
    Strong,
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    shape: Shape = MaterialTheme.shapes.large,
    level: GlassLevel = GlassLevel.Normal,
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val designTokens = ZouDesignTokens.colors
    val motion = LocalZouMotion.current
    val resolvedLevel = if (strong) GlassLevel.Strong else level
    val containerColor = when (resolvedLevel) {
        GlassLevel.Weak -> designTokens.backgroundRaised
        GlassLevel.Normal -> designTokens.surface
        GlassLevel.Strong -> designTokens.surfaceVariant
    }
    val borderColor = when (resolvedLevel) {
        GlassLevel.Weak -> designTokens.glassBorderSoft.copy(alpha = 0.68f)
        GlassLevel.Normal -> designTokens.glassBorderSoft
        GlassLevel.Strong -> designTokens.glassBorder
    }
    val shadowElevation = when (resolvedLevel) {
        GlassLevel.Weak -> 2.dp
        GlassLevel.Normal -> 4.dp
        GlassLevel.Strong -> 7.dp
    }

    val animatedContainerColor = animateColorAsState(
        targetValue = containerColor,
        animationSpec = motion.colorShift,
        label = "glass_surface_container_color",
    ).value
    val animatedBorderBaseColor = animateColorAsState(
        targetValue = borderColor,
        animationSpec = motion.colorShift,
        label = "glass_surface_border_color",
    ).value
    val animatedAccentColor = animateColorAsState(
        targetValue = accentColor ?: Color.Transparent,
        animationSpec = motion.colorShift,
        label = "glass_surface_accent_color",
    ).value

    val finalContainerColor = if (accentColor != null) {
        lerp(animatedContainerColor, animatedAccentColor, 0.04f)
    } else {
        animatedContainerColor
    }

    val finalBorderColor = if (accentColor != null) {
        lerp(animatedBorderBaseColor, animatedAccentColor, 0.12f)
    } else {
        animatedBorderBaseColor
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = finalContainerColor,
        shadowElevation = shadowElevation,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, finalBorderColor),
    ) {
        Box(content = content)
    }
}
