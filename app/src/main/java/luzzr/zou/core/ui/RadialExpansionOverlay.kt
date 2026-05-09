package luzzr.zou.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class RadialExpansionAnchor(
    val color: Color,
    val origin: Offset,
)

internal sealed interface RadialExpansionRequest {
    val token: Long
    val color: Color
    val origin: Offset

    data class Expand(
        override val token: Long,
        override val color: Color,
        override val origin: Offset,
        val onExpanded: () -> Unit,
    ) : RadialExpansionRequest

    data class Collapse(
        override val token: Long,
        override val color: Color,
        override val origin: Offset,
        val onCollapsed: () -> Unit,
    ) : RadialExpansionRequest
}

@Stable
class RadialExpansionController {
    private var nextToken = 0L
    internal var request by mutableStateOf<RadialExpansionRequest?>(null)
        private set
    private var lastAnchor by mutableStateOf<RadialExpansionAnchor?>(null)

    fun launch(
        color: Color,
        origin: Offset?,
        onExpanded: () -> Unit,
    ) {
        if (origin == null) {
            onExpanded()
            return
        }
        lastAnchor = RadialExpansionAnchor(
            color = color,
            origin = origin,
        )
        request = RadialExpansionRequest.Expand(
            token = ++nextToken,
            color = color,
            origin = origin,
            onExpanded = onExpanded,
        )
    }

    fun collapse(
        onCollapsed: () -> Unit,
        color: Color? = null,
    ) {
        val anchor = lastAnchor
        if (anchor == null) {
            println("[RadialExpansion] collapse skipped: lastAnchor is null")
            onCollapsed()
            return
        }
        println("[RadialExpansion] collapse triggered: origin=${anchor.origin}")
        request = RadialExpansionRequest.Collapse(
            token = ++nextToken,
            color = color ?: anchor.color,
            origin = anchor.origin,
            onCollapsed = onCollapsed,
        )
    }

    internal fun clear(token: Long) {
        if (request?.token == token) {
            request = null
        }
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
    val request = controller.request

    key(request?.token) {
        val radiusProgress = remember { Animatable(0f) }
        val alpha = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            val activeRequest = request ?: return@LaunchedEffect
            when (activeRequest) {
                is RadialExpansionRequest.Expand -> {
                    radiusProgress.snapTo(0f)
                    alpha.snapTo(0.9f)

                    val navigateJob = launch {
                        delay(MotionTokens.DurationFabNavigateDelay.toLong())
                        activeRequest.onExpanded()
                    }

                    radiusProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationFabRadial,
                            easing = MotionTokens.EasingEmphasizedDecelerate,
                        ),
                    )
                    navigateJob.join()
                    delay(MotionTokens.DurationFabOverlayHold.toLong())
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationFabOverlayFade,
                            easing = MotionTokens.EasingEmphasizedDecelerate,
                        ),
                    )
                }

                is RadialExpansionRequest.Collapse -> {
                    println("[RadialExpansion] Collapse animation START: radius=${radiusProgress.value}, alpha=${alpha.value}")
                    // 先恢复可见，确保下一帧 Canvas 绘制
                    radiusProgress.snapTo(1f)
                    alpha.snapTo(0.9f)
                    delay(100) // 等 ~6 帧确保 Canvas 重绘完成

                    radiusProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationFabRadial + 200,
                            easing = MotionTokens.EasingAccelerate,
                        ),
                    )
                    println("[RadialExpansion] Collapse radius done")
                    delay(MotionTokens.DurationFabCollapseDelay.toLong())
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationFabOverlayFade,
                            easing = MotionTokens.EasingEmphasizedDecelerate,
                        ),
                    )
                    println("[RadialExpansion] Collapse alpha done, calling onCollapsed")
                    activeRequest.onCollapsed()
                }
            }
            controller.clear(activeRequest.token)
        }

        if (request != null) {
            Canvas(
                modifier = modifier.fillMaxSize(),
            ) {
                val maxRadius = listOf(
                    hypot(request.origin.x.toDouble(), request.origin.y.toDouble()),
                    hypot((size.width - request.origin.x).toDouble(), request.origin.y.toDouble()),
                    hypot(request.origin.x.toDouble(), (size.height - request.origin.y).toDouble()),
                    hypot((size.width - request.origin.x).toDouble(), (size.height - request.origin.y).toDouble()),
                ).maxOrNull()?.toFloat() ?: 0f

                drawCircle(
                    color = request.color.copy(alpha = alpha.value),
                    radius = maxRadius * radiusProgress.value,
                    center = request.origin,
                )
            }
        }
    }
}
