package luzzr.zou.core.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

object MotionTokens {
    // ─── Spring (弹性) ─────────────────────────────────────
    /** 强弹性 — 按钮点击、复选框、弹跳感 */
    val SpringBouncy = spring<Float>(
        dampingRatio = 0.4f,
        stiffness = 280f,
    )

    /** 中弹性 — 卡片弹出、FAB 展开 */
    val SpringBouncyDp = spring<Dp>(
        dampingRatio = 0.5f,
        stiffness = 300f,
    )

    /** 柔和弹性 — 进度条、数字变化 */
    val SpringGentle = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = 220f,
    )

    /** 无弹平滑 — Tab 指示器、偏移 */
    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 280f,
    )

    val SpringSmoothDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 280f,
    )

    /** 超顺滑 — 淡入淡出 */
    val SpringMellow = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 150f,
    )

    // ─── Easing（贝塞尔曲线） ─────────────────────────────
    /** Material 3 Emphasized — 大多数入场 */
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.85f, 0f, 1f)
    /** 加速退出 */
    val EasingAccelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    /** 标准缓入缓出 */
    val EasingStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    /** 强强调 — 卡片入场、弹窗 */
    val EasingEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    /** FAB 展开圆圈水波纹专用曲线：缓入加速，前段极缓确保过程感可见，后段极速填满全屏，与 EasingAccelerate 镜像 */
    val EasingFabExpand = CubicBezierEasing(0.65f, 0.0f, 0.25f, 1.0f)
    /** 线性 — 无限循环/ shimmer */
    val EasingLinear = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)

    // ─── Duration（时长） ─────────────────────────────────
    const val DurationInstant = 80
    const val DurationShort = 200
    const val DurationMedium = 340
    const val DurationLong = 500
    const val DurationExtraLong = 800

    // Navigation / Canvas
    const val DurationNavExit = 90
    const val DurationNavEnter = 340
    const val DurationNavOverlapDelay = 30
    const val DurationDepthExit = 220
    const val DurationCanvasSlide = 360
    const val DurationDepthEnter = 340

    // Sections
    const val DurationSectionEnter = 320
    const val DurationSectionStagger = 80
    const val DurationSectionStaggerFast = 50

    // Form
    const val DurationFormStep = 340

    // FAB / Radial
    const val DurationFabRadial = 420
    const val DurationFabCollapseDelay = 72
    const val DurationFabNavigateDelay = 100
    const val DurationFabOverlayHold = 40
    const val DurationFabOverlayFade = 150

    // Shimmer
    const val DurationShimmerCycle = 1200

    // Canvas / Parallax
    const val CanvasParallaxFactor = 0.15f
    const val CanvasAdjacentScale = 0.94f
    const val CanvasAdjacentAlpha = 0.75f
    const val CanvasDepthOverlayTopLevel = 0.018f
    const val CanvasDepthOverlayDetail = 0.042f
}
