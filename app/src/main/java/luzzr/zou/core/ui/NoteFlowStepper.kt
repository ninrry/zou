package luzzr.zou.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NoteFlowStepBar(
    steps: List<String>,
    currentStep: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onStepSelected: ((Int) -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space8),
    ) {
        steps.forEachIndexed { index, title ->
            val selected = index == currentStep
            val selectionProgress by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = MotionTokens.SpringSmooth,
                label = "step_selection_progress_$index",
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationFormStep,
                    easing = MotionTokens.EasingEmphasized,
                ),
                label = "step_text_color_$index",
            )
            GlassSurface(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        val scale = 0.985f + (selectionProgress * 0.015f)
                        scaleX = scale
                        scaleY = scale
                    },
                accentColor = if (selected) accentColor else null,
                level = if (selected) GlassLevel.Normal else GlassLevel.Weak,
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onStepSelected != null) {
                            onStepSelected?.invoke(index)
                        }
                        .padding(horizontal = LayoutTokens.Space8, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun NoteFlowStepBottomBar(
    primaryLabel: String,
    primaryAccentColor: Color,
    modifier: Modifier = Modifier,
    previousVisible: Boolean = false,
    previousEnabled: Boolean = true,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false,
    previousLabel: String = "上一步",
    onPreviousClick: () -> Unit = {},
    onPrimaryClick: () -> Unit,
    primaryTestTag: String,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.Strong,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.Space20, vertical = LayoutTokens.Space12),
            horizontalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (previousVisible) {
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(LayoutTokens.Space24 + LayoutTokens.Space20),
                    onClick = onPreviousClick,
                    enabled = previousEnabled,
                    colors = noteFlowOutlinedButtonColors(),
                ) {
                    Text(previousLabel, maxLines = 1)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(LayoutTokens.Space24 + LayoutTokens.Space20)
                    .testTag(primaryTestTag),
                onClick = onPrimaryClick,
                enabled = primaryEnabled,
                colors = noteFlowButtonColors(primaryAccentColor),
            ) {
                if (primaryLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = LayoutTokens.Space8 / 4,
                    )
                } else {
                    Text(primaryLabel, maxLines = 1)
                }
            }
        }
    }
}
