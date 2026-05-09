package luzzr.zou.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
    return fadeIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
    ) + slideInVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        initialOffsetY = { it / 6 },
    ) + scaleIn(
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        initialScale = 0.94f,
    )
}
