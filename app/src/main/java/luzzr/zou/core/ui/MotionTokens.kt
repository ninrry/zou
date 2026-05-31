package luzzr.zou.core.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp

@Immutable
data class ZouMotionSpec(
    val press: FiniteAnimationSpec<Float>,
    val pressDp: FiniteAnimationSpec<Dp>,
    val colorShift: FiniteAnimationSpec<Color>,
    val listEnter: FiniteAnimationSpec<Float>,
    val listEnterOffset: FiniteAnimationSpec<IntOffset>,
    val listExit: FiniteAnimationSpec<Float>,
    val listExitSize: FiniteAnimationSpec<IntSize>,
    val pageEnter: FiniteAnimationSpec<Float>,
    val pageEnterOffset: FiniteAnimationSpec<IntOffset>,
    val pageExit: FiniteAnimationSpec<Float>,
    val pageExitOffset: FiniteAnimationSpec<IntOffset>,
    val tabSwitch: FiniteAnimationSpec<Float>,
    val tabSwitchDp: FiniteAnimationSpec<Dp>,
    val fabReveal: FiniteAnimationSpec<Float>,
    val fabCollapse: FiniteAnimationSpec<Float>,
    val fabMenu: FiniteAnimationSpec<Float>,
    val fabMenuOffset: FiniteAnimationSpec<IntOffset>,
    val formStep: FiniteAnimationSpec<Float>,
    val shimmerDurationMillis: Int,
    val listStaggerMillis: Int,
    val listEnterOffsetDp: Dp,
) {
    companion object {
        val Default = ZouMotionSpec(
            press = spring(
                dampingRatio = 0.46f,
                stiffness = 300f,
            ),
            pressDp = spring(
                dampingRatio = 0.62f,
                stiffness = 300f,
            ),
            colorShift = tween(
                durationMillis = 360,
                easing = MotionTokens.EasingEmphasized,
            ),
            listEnter = tween(
                durationMillis = 380,
                easing = MotionTokens.EasingEmphasized,
            ),
            listEnterOffset = tween(
                durationMillis = 380,
                easing = MotionTokens.EasingEmphasized,
            ),
            listExit = tween(
                durationMillis = 260,
                easing = MotionTokens.EasingStandard,
            ),
            listExitSize = tween(
                durationMillis = 260,
                easing = MotionTokens.EasingStandard,
            ),
            pageEnter = tween(
                durationMillis = 360,
                easing = MotionTokens.EasingEmphasized,
            ),
            pageEnterOffset = tween(
                durationMillis = 360,
                easing = MotionTokens.EasingEmphasized,
            ),
            pageExit = tween(
                durationMillis = 220,
                easing = MotionTokens.EasingStandard,
            ),
            pageExitOffset = tween(
                durationMillis = 220,
                easing = MotionTokens.EasingStandard,
            ),
            tabSwitch = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 280f,
            ),
            tabSwitchDp = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 280f,
            ),
            fabReveal = tween(
                durationMillis = 420,
                easing = MotionTokens.EasingEmphasizedDecelerate,
            ),
            fabCollapse = tween(
                durationMillis = 420,
                easing = MotionTokens.EasingAccelerate,
            ),
            fabMenu = spring(
                dampingRatio = 0.76f,
                stiffness = 170f,
            ),
            fabMenuOffset = spring(
                dampingRatio = 0.76f,
                stiffness = 170f,
            ),
            formStep = tween(
                durationMillis = 340,
                easing = MotionTokens.EasingEmphasized,
            ),
            shimmerDurationMillis = 1200,
            listStaggerMillis = 36,
            listEnterOffsetDp = LayoutTokens.Space24,
        )
    }
}

val LocalZouMotion = staticCompositionLocalOf { ZouMotionSpec.Default }

object MotionTokens {
    val SpringBouncy = spring<Float>(
        dampingRatio = 0.46f,
        stiffness = 300f,
    )

    val SpringBouncyDp = spring<Dp>(
        dampingRatio = 0.58f,
        stiffness = 300f,
    )

    val SpringGentle = spring<Float>(
        dampingRatio = 0.68f,
        stiffness = 220f,
    )

    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 280f,
    )

    val SpringSmoothDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 280f,
    )

    val SpringMellow = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 150f,
    )

    val EasingEmphasized: Easing = CubicBezierEasing(0.2f, 0.85f, 0f, 1f)
    val EasingAccelerate: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val EasingStandard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingEmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EasingLinear: Easing = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)

    const val DurationInstant = 80
    const val DurationShort = 200
    const val DurationMedium = 340
    const val DurationLong = 500
    const val DurationExtraLong = 800

    const val DurationNavExit = 90
    const val DurationNavEnter = 340
    const val DurationNavOverlapDelay = 30
    const val DurationDepthExit = 220
    const val DurationCanvasSlide = 360
    const val DurationDepthEnter = 340

    const val DurationSectionEnter = 320
    const val DurationSectionStagger = 80
    const val DurationSectionStaggerFast = 50

    const val DurationFormStep = 340

    const val DurationFabRadial = 420
    const val DurationFabCollapseDelay = 72
    const val DurationFabNavigateDelay = 100
    const val DurationFabOverlayHold = 40
    const val DurationFabOverlayFade = 150

    const val DurationShimmerCycle = 1200

    const val CanvasParallaxFactor = 0.15f
    const val CanvasAdjacentScale = 0.94f
    const val CanvasAdjacentAlpha = 0.75f
    const val CanvasDepthOverlayTopLevel = 0.018f
    const val CanvasDepthOverlayDetail = 0.042f
}
