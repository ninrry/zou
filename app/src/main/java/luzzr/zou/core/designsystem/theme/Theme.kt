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

private val LightColorScheme = lightColorScheme(
    primary = NoteFlowTodayAccent,
    onPrimary = LightNoteFlowUiColors.onAccent,
    secondary = NoteFlowTaskAccent,
    tertiary = NoteFlowHabitAccent,
    background = LightNoteFlowUiColors.background,
    onBackground = LightNoteFlowUiColors.textPrimary,
    surface = LightNoteFlowUiColors.surface,
    onSurface = LightNoteFlowUiColors.textPrimary,
    surfaceVariant = LightNoteFlowUiColors.surfaceVariant,
    onSurfaceVariant = LightNoteFlowUiColors.textSecondary,
    outline = LightNoteFlowUiColors.outlineSoft,
    outlineVariant = LightNoteFlowUiColors.outlineSoft.copy(alpha = 0.72f),
    primaryContainer = NoteFlowTodayAccentSoft,
    secondaryContainer = NoteFlowTaskAccentSoft,
    tertiaryContainer = NoteFlowHabitAccentSoft,
    error = NoteFlowDanger,
)

private val DarkColorScheme = darkColorScheme(
    primary = NoteFlowTaskAccent,
    onPrimary = DarkNoteFlowUiColors.onAccent,
    secondary = NoteFlowHabitAccent,
    tertiary = NoteFlowNoteAccent,
    background = DarkNoteFlowUiColors.background,
    onBackground = DarkNoteFlowUiColors.textPrimary,
    surface = DarkNoteFlowUiColors.surface,
    onSurface = DarkNoteFlowUiColors.textPrimary,
    surfaceVariant = DarkNoteFlowUiColors.surfaceVariant,
    onSurfaceVariant = DarkNoteFlowUiColors.textSecondary,
    outline = DarkNoteFlowUiColors.outlineSoft,
    outlineVariant = DarkNoteFlowUiColors.outlineSoft.copy(alpha = 0.82f),
    primaryContainer = Color(0xFF37315A),
    secondaryContainer = Color(0xFF2D433B),
    tertiaryContainer = Color(0xFF4A4126),
    error = Color(0xFFE08B8B),
)

@Composable
fun NoteFlowTheme(
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

    CompositionLocalProvider(LocalNoteFlowUiColors provides noteFlowUiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoteFlowTypography,
            shapes = NoteFlowShapes,
            content = content,
        )
    }
}
