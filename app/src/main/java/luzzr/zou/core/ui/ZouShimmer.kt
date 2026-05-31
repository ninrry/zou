package luzzr.zou.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import luzzr.zou.core.designsystem.theme.ZouDesignTokens

/**
 * A shimmer/skeleton placeholder for loading states.
 * Features a smooth gradient sweep animation that scans left-to-right.
 */
@Composable
fun ZouShimmer(
    modifier: Modifier = Modifier,
    baseColor: Color = ZouDesignTokens.colors.glassSurface.copy(alpha = 0.45f),
    highlightColor: Color = ZouDesignTokens.colors.glassSurface.copy(alpha = 0.75f),
) {
    val motion = LocalZouMotion.current
    val transition = rememberInfiniteTransition(label = "shimmer_sweep")
    val progress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = motion.shimmerDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    )
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
    ) {
        val width = size.width
        val height = size.height
        val highlightWidth = width * 0.4f
        val xPos = progress * width

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    baseColor,
                    highlightColor,
                    baseColor,
                    baseColor,
                ),
                start = Offset(xPos - highlightWidth, 0f),
                end = Offset(xPos + highlightWidth, 0f),
            ),
            size = size,
        )
    }
}

@Composable
fun ZouShimmerCard(
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(18.dp),
    ) {
        ZouShimmer(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(20.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        repeat(lines) {
            ZouShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZouShimmer(
                modifier = Modifier
                    .width(60.dp)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            ZouShimmer(
                modifier = Modifier
                    .width(40.dp)
                    .height(14.dp),
            )
        }
    }
}

@Composable
fun ZouShimmerList(
    itemCount: Int = 4,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        repeat(itemCount) {
            ZouShimmerCard()
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
