package luzzr.zou.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ZouEmptyStateCard(
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MotionTokens.SpringBouncy,
        label = "empty_icon_scale",
    )
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        accentColor = accentColor,
        strong = true,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationMedium,
                    easing = MotionTokens.EasingEmphasized,
                ),
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationMedium,
                    delayMillis = 60,
                    easing = MotionTokens.EasingEmphasized,
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(iconScale),
                        tint = accentColor,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (actionLabel != null && actionTestTag != null && onActionClick != null) {
                    OutlinedButton(
                        modifier = Modifier.testTag(actionTestTag),
                        onClick = onActionClick,
                        colors = noteFlowOutlinedButtonColors(),
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
