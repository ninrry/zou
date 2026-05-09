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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ModuleFab(
    accentColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    testTag: String,
    enableRadialExpansion: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    val radialExpansionController = LocalRadialExpansionController.current
    val fabCenter = remember { mutableStateOf<Offset?>(null) }
    var isRotated by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val iconRotation by animateFloatAsState(
        targetValue = if (isRotated) 45f else 0f,
        animationSpec = spring(
            dampingRatio = 0.35f,
            stiffness = 320f,
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
                fabCenter.value = coordinates.boundsInRoot().center
            }
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = accentColor.copy(alpha = 0.28f),
                spotColor = accentColor.copy(alpha = 0.24f),
            )
            .size(64.dp)
            .testTag(testTag),
        interactionSource = interactionSource,
        onClick = {
            isRotated = !isRotated
            coroutineScope.launch {
                delay(300)
                isRotated = !isRotated
            }
            if (enableRadialExpansion) {
                radialExpansionController?.launch(
                    color = accentColor,
                    origin = fabCenter.value,
                    onExpanded = onClick,
                ) ?: onClick()
            } else {
                onClick()
            }
        },
        shape = CircleShape,
        containerColor = accentColor,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.rotate(iconRotation),
            contentDescription = contentDescription,
        )
    }
}
