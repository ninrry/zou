package luzzr.zou.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun ZouStaggeredReveal(
    revealKey: Any,
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by rememberSaveable(revealKey) { mutableStateOf(false) }

    LaunchedEffect(revealKey) {
        if (!visible) {
            delay((index * MotionTokens.DurationSectionStagger).toLong())
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = defaultZouEnterTransition(index),
    ) {
        content()
    }
}

private fun defaultZouEnterTransition(index: Int): EnterTransition {
    val delayMillis = index * MotionTokens.DurationSectionStagger
    return fadeIn(
        animationSpec = tween(
            durationMillis = MotionTokens.DurationSectionEnter,
            delayMillis = delayMillis,
            easing = MotionTokens.EasingEmphasized,
        ),
    ) + slideInVertically(
        animationSpec = tween(
            durationMillis = MotionTokens.DurationSectionEnter,
            delayMillis = delayMillis,
            easing = MotionTokens.EasingEmphasized,
        ),
        initialOffsetY = { it / 8 },
    ) + scaleIn(
        animationSpec = tween(
            durationMillis = MotionTokens.DurationSectionEnter,
            delayMillis = delayMillis,
            easing = MotionTokens.EasingEmphasized,
        ),
        initialScale = 0.985f,
    )
}
