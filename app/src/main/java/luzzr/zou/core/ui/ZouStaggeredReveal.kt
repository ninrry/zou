package luzzr.zou.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween

@Composable
fun ZouStaggeredReveal(
    revealKey: Any,
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by rememberSaveable(revealKey) { mutableStateOf(false) }

    LaunchedEffect(revealKey) {
        if (!visible) {
            // 将错开延迟从原来的 80ms 缩短为极具奶油流畅度的 36ms
            delay((index * 36).toLong())
            visible = true
        }
    }

    val density = LocalDensity.current
    val offsetY = remember(density) { with(density) { 24.dp.roundToPx() } }
    val enterTransition = remember(offsetY) {
        val curve = MotionTokens.EasingEmphasized
        fadeIn(
            animationSpec = tween(
                durationMillis = 380, // 时长对齐为 380ms，使淡入与滑移、缩放插轨一致，消除割裂感
                easing = curve,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 380,
                easing = curve,
            ),
            initialOffsetY = { offsetY }, // 统一为绝对的 24.dp 位移，在各种屏幕尺寸下维持一致物理滑行感
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = 380,
                easing = curve,
            ),
            initialScale = 0.94f,
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
    ) {
        content()
    }
}

