package luzzr.zou.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import luzzr.zou.core.ui.LocalZouMotion
import luzzr.zou.core.ui.ZouMotionSpec

private val LightColorScheme = lightColorScheme(
    primary = ZouTodayAccent,
    onPrimary = LightZouUiColors.onAccent,
    secondary = ZouTaskAccent,
    tertiary = ZouHabitAccent,
    background = LightZouUiColors.background,
    onBackground = LightZouUiColors.textPrimary,
    surface = LightZouUiColors.surface,
    onSurface = LightZouUiColors.textPrimary,
    surfaceVariant = LightZouUiColors.surfaceVariant,
    onSurfaceVariant = LightZouUiColors.textSecondary,
    outline = LightZouUiColors.outlineSoft,
    outlineVariant = LightZouUiColors.outlineSoft.copy(alpha = 0.72f),
    primaryContainer = ZouTodayAccentSoft,
    secondaryContainer = ZouTaskAccentSoft,
    tertiaryContainer = ZouHabitAccentSoft,
    error = ZouDanger,
)

private val DarkColorScheme = darkColorScheme(
    primary = ZouTodayAccent,
    onPrimary = DarkZouUiColors.onAccent,
    secondary = ZouTaskAccent,
    tertiary = ZouHabitAccent,
    background = DarkZouUiColors.background,
    onBackground = DarkZouUiColors.textPrimary,
    surface = DarkZouUiColors.surface,
    onSurface = DarkZouUiColors.textPrimary,
    surfaceVariant = DarkZouUiColors.surfaceVariant,
    onSurfaceVariant = DarkZouUiColors.textSecondary,
    outline = DarkZouUiColors.outlineSoft,
    outlineVariant = DarkZouUiColors.outlineSoft.copy(alpha = 0.82f),
    primaryContainer = Color(0xFF233842),
    secondaryContainer = Color(0xFF352F4B),
    tertiaryContainer = Color(0xFF273A31),
    error = DarkZouUiColors.danger,
)

@Composable
fun ZouTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val noteFlowUiColors = noteFlowUiColors(useDarkTheme = useDarkTheme)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDarkTheme -> {
            dynamicDarkColorScheme(LocalContext.current)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(LocalContext.current)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalZouUiColors provides noteFlowUiColors,
        LocalZouMotion provides ZouMotionSpec.Default,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZouTypography,
            shapes = ZouShapes,
            content = content,
        )
    }
}
