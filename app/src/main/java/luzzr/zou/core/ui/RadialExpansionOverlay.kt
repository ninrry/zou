package luzzr.zou.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

data class RadialExpansionAnchor(
    val color: Color,
    val origin: Offset,
)

@Stable
class RadialExpansionController {
    var lastAnchor by mutableStateOf<RadialExpansionAnchor?>(null)
        private set

    fun launch(
        color: Color,
        origin: Offset?,
        onNavigate: () -> Unit,
    ) {
        // 强制覆盖 lastAnchor，防止被前一次其他页面的老旧 FAB 起爆坐标所污染
        lastAnchor = RadialExpansionAnchor(
            color = color,
            origin = origin ?: Offset.Unspecified
        )
        // Immediately navigate; the destination screen will circular reveal itself.
        onNavigate()
    }

    fun collapse(
        onCollapsed: () -> Unit,
        color: Color? = null,
    ) {
        // Immediately trigger collapse navigation.
        onCollapsed()
    }
}

val LocalRadialExpansionController = staticCompositionLocalOf<RadialExpansionController?> { null }

@Composable
fun rememberRadialExpansionController(): RadialExpansionController = remember { RadialExpansionController() }

@Composable
fun ProvideRadialExpansionController(
    controller: RadialExpansionController,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRadialExpansionController provides controller) {
        content()
    }
}

@Composable
fun RadialExpansionOverlay(
    controller: RadialExpansionController,
    modifier: Modifier = Modifier,
) {
    // Intentionally empty. We now use Modifier.circularReveal directly on the destination screen instead of a fake overlay.
}

fun Modifier.circularReveal(
    progress: Float,
    origin: Offset?,
    backgroundColor: Color = Color.Unspecified,
) = this.then(
    Modifier.drawWithContent {
        val resolvedOrigin = if (origin == null || origin == Offset.Unspecified) {
            Offset(
                x = size.width / 2f,
                y = size.height - 64.dp.toPx(),
            )
        } else {
            origin
        }
        val safeProgress = progress.coerceIn(0f, 1f)
        if (safeProgress == 0f) return@drawWithContent
        if (safeProgress >= 1f) {
            if (backgroundColor != Color.Unspecified) {
                drawRect(backgroundColor)
            }
            drawContent()
            return@drawWithContent
        }
        val maxRadius = hypot(
            maxOf(resolvedOrigin.x, size.width - resolvedOrigin.x),
            maxOf(resolvedOrigin.y, size.height - resolvedOrigin.y),
        )
        val currentRadius = maxRadius * safeProgress
        val path = Path().apply {
            addOval(Rect(center = resolvedOrigin, radius = currentRadius))
        }
        clipPath(path) {
            if (backgroundColor != Color.Unspecified) {
                drawRect(backgroundColor)
            }
            this@drawWithContent.drawContent()
        }
    },
)
