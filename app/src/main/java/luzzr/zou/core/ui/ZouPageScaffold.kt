package luzzr.zou.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import luzzr.zou.core.designsystem.theme.ZouDesignTokens

@Composable
fun ZouPageScaffold(
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        content = content,
    )
}

@Composable
fun ZouListHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    count: Int? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null,
) {
    val designTokens = ZouDesignTokens.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = designTokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = designTokens.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (count != null) {
            ZouMetaChip(
                text = count.toString(),
                accentColor = accentColor,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun ZouBottomActionBar(
    primaryLabel: String,
    primaryAccentColor: Color,
    onPrimaryClick: () -> Unit,
    primaryTestTag: String,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false,
    secondaryLabel: String = "取消",
    secondaryEnabled: Boolean = true,
    onSecondaryClick: () -> Unit = {},
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.Normal,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.Space20, vertical = LayoutTokens.Space12),
            horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                onClick = onSecondaryClick,
                enabled = secondaryEnabled,
                colors = noteFlowOutlinedButtonColors(),
            ) {
                Text(secondaryLabel, maxLines = 1)
            }
            ZouPrimaryActionButton(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag(primaryTestTag),
                label = primaryLabel,
                accentColor = primaryAccentColor,
                enabled = primaryEnabled,
                loading = primaryLoading,
                onClick = onPrimaryClick,
            )
        }
    }
}

@Composable
fun RowScope.ZouPrimaryActionButton(
    label: String,
    accentColor: Color,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        colors = noteFlowButtonColors(accentColor),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = LayoutTokens.Space8 / 4,
            )
        } else {
            Text(label, maxLines = 1)
        }
    }
}

@Composable
fun ZouAnimatedGlassSurface(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    level: GlassLevel = GlassLevel.Normal,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        accentColor = accentColor,
        level = level,
        content = content,
    )
}
