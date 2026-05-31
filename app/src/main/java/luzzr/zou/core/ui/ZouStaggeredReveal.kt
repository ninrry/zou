package luzzr.zou.core.ui

import androidx.compose.animation.AnimatedVisibility
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
import kotlinx.coroutines.delay

@Composable
fun ZouStaggeredReveal(
    revealKey: Any,
    index: Int,
    content: @Composable () -> Unit,
) {
    val motion = LocalZouMotion.current
    var visible by rememberSaveable(revealKey) { mutableStateOf(false) }

    LaunchedEffect(revealKey) {
        if (!visible) {
            delay((index * motion.listStaggerMillis).toLong())
            visible = true
        }
    }

    val density = LocalDensity.current
    val offsetY = remember(density, motion.listEnterOffsetDp) {
        with(density) { motion.listEnterOffsetDp.roundToPx() }
    }
    val enterTransition = remember(offsetY, motion) {
        fadeIn(
            animationSpec = motion.listEnter,
        ) + slideInVertically(
            animationSpec = motion.listEnterOffset,
            initialOffsetY = { offsetY },
        ) + scaleIn(
            animationSpec = motion.listEnter,
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
