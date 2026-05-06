package luzzr.zou.core.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

object MotionTokens {
    val SpringBouncy = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = 400f,
    )

    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 300f,
    )

    val SpringSmoothDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 300f,
    )

    val EasingEmphasized = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
    val EasingAccelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val EasingStandard = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

    const val DurationShort = 200
    const val DurationMedium = 340
    const val DurationLong = 500
    const val DurationNavExit = 90
    const val DurationNavEnter = 340
    const val DurationNavOverlapDelay = 30
    const val DurationDepthExit = 220
    const val DurationCanvasSlide = 360
    const val DurationDepthEnter = 340
    const val DurationSectionEnter = 320
    const val DurationSectionStagger = 60
    const val DurationFormStep = 340
    const val DurationFabRadial = 380
    const val DurationFabCollapseDelay = 72
    const val DurationFabNavigateDelay = 120
    const val DurationFabOverlayHold = 60
    const val DurationFabOverlayFade = 180
    const val CanvasParallaxFactor = 0.08f
    const val CanvasAdjacentScale = 0.965f
    const val CanvasAdjacentAlpha = 0.82f
    const val CanvasDepthOverlayTopLevel = 0.018f
    const val CanvasDepthOverlayDetail = 0.042f
}
