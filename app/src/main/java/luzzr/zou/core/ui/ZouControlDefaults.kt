package luzzr.zou.core.ui

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import luzzr.zou.core.designsystem.theme.ZouDesignTokens

@Composable
fun noteFlowSwitchColors(
    accentColor: Color = MaterialTheme.colorScheme.primary,
): SwitchColors {
    val designTokens = ZouDesignTokens.colors
    return SwitchDefaults.colors(
        checkedThumbColor = designTokens.backgroundRaised,
        checkedTrackColor = accentColor.copy(alpha = 0.82f),
        checkedBorderColor = accentColor.copy(alpha = 0.42f),
        checkedIconColor = accentColor,
        uncheckedThumbColor = designTokens.backgroundRaised,
        uncheckedTrackColor = designTokens.surfaceVariant.copy(alpha = 0.64f),
        uncheckedBorderColor = designTokens.outlineSoft.copy(alpha = 0.68f),
        uncheckedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
        disabledCheckedThumbColor = designTokens.backgroundRaised.copy(alpha = 0.64f),
        disabledCheckedTrackColor = accentColor.copy(alpha = 0.42f),
        disabledCheckedBorderColor = accentColor.copy(alpha = 0.30f),
        disabledCheckedIconColor = accentColor.copy(alpha = 0.64f),
        disabledUncheckedThumbColor = designTokens.backgroundRaised.copy(alpha = 0.64f),
        disabledUncheckedTrackColor = designTokens.surfaceVariant.copy(alpha = 0.46f),
        disabledUncheckedBorderColor = designTokens.outlineSoft.copy(alpha = 0.42f),
        disabledUncheckedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
    )
}

@Composable
fun noteFlowButtonColors(
    accentColor: Color = MaterialTheme.colorScheme.primary,
): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = accentColor,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = accentColor.copy(alpha = 0.44f),
    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
)

@Composable
fun noteFlowOutlinedButtonColors(): ButtonColors {
    val designTokens = ZouDesignTokens.colors
    return ButtonDefaults.outlinedButtonColors(
        containerColor = designTokens.backgroundRaised,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = designTokens.surfaceVariant.copy(alpha = 0.8f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
    )
}

@Composable
fun noteFlowFilterChipColors(
    accentColor: Color = MaterialTheme.colorScheme.primary,
): SelectableChipColors {
    val designTokens = ZouDesignTokens.colors
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = accentColor.copy(alpha = 0.18f),
        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        selectedLeadingIconColor = accentColor,
        selectedTrailingIconColor = accentColor,
        containerColor = designTokens.backgroundRaised,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor = designTokens.surfaceVariant.copy(alpha = 0.8f),
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
    )
}

@Composable
fun noteFlowCheckboxColors(
    accentColor: Color = MaterialTheme.colorScheme.primary,
): CheckboxColors {
    val designTokens = ZouDesignTokens.colors
    return CheckboxDefaults.colors(
        checkedColor = accentColor.copy(alpha = 0.86f),
        checkmarkColor = designTokens.onAccent,
        uncheckedColor = designTokens.surfaceVariant.copy(alpha = 0.72f),
        disabledCheckedColor = accentColor.copy(alpha = 0.42f),
        disabledUncheckedColor = designTokens.surfaceVariant.copy(alpha = 0.36f),
    )
}
