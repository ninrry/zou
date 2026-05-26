package luzzr.zou.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import luzzr.zou.core.designsystem.theme.ZouOnAccent

@Composable
fun ModuleFab(
    accentColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    testTag: String,
    enableRadialExpansion: Boolean = true,
    containerColor: Color = accentColor,
    contentColor: Color = ZouOnAccent,
    isRotated: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    val radialExpansionController = LocalRadialExpansionController.current
    val fabCenter = remember { mutableStateOf<Offset?>(null) }
    val iconRotation by animateFloatAsState(
        targetValue = if (isRotated) 135f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 180f,
        ),
        label = "module_fab_icon_rotation",
    )

    FloatingActionButton(
        modifier = modifier
            .noteFlowPressScale(
                interactionSource = interactionSource,
                pressedScale = 0.9f,
            )
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val size = coordinates.size
                fabCenter.value = Offset(
                    x = position.x + size.width / 2f,
                    y = position.y + size.height / 2f
                )
            }
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = containerColor.copy(alpha = 0.28f),
                spotColor = containerColor.copy(alpha = 0.24f),
            )
            .size(64.dp)
            .testTag(testTag),
        interactionSource = interactionSource,
        onClick = {
            if (enableRadialExpansion) {
                radialExpansionController?.launch(
                    color = containerColor,
                    origin = fabCenter.value,
                    onNavigate = onClick,
                ) ?: onClick()
            } else {
                onClick()
            }
        },
        shape = CircleShape,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.rotate(iconRotation),
            contentDescription = contentDescription,
        )
    }
}
