package luzzr.zou.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import luzzr.zou.core.designsystem.theme.NoteFlowDesignTokens

@Composable
fun noteFlowOutlinedTextFieldColors(): TextFieldColors {
    val designTokens = NoteFlowDesignTokens.colors
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = designTokens.backgroundRaised,
        unfocusedContainerColor = designTokens.backgroundRaised,
        disabledContainerColor = designTokens.surfaceVariant.copy(alpha = 0.8f),
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
        unfocusedBorderColor = designTokens.outlineSoft.copy(alpha = 0.74f),
        disabledBorderColor = designTokens.outlineSoft.copy(alpha = 0.36f),
        focusedTextColor = designTokens.textPrimary,
        unfocusedTextColor = designTokens.textPrimary,
        focusedLabelColor = designTokens.textSecondary,
        unfocusedLabelColor = designTokens.textSecondary,
        focusedPlaceholderColor = designTokens.textSecondary.copy(alpha = 0.82f),
        unfocusedPlaceholderColor = designTokens.textSecondary.copy(alpha = 0.74f),
        focusedSupportingTextColor = designTokens.textSecondary,
        unfocusedSupportingTextColor = designTokens.textSecondary,
        cursorColor = MaterialTheme.colorScheme.primary,
    )
}
