package com.jzbrooks.vgo.core

sealed interface Paint

data class GradientStop(
    val offset: Float,
    val color: Color,
)

enum class TileMode {
    CLAMP,
    REPEAT,
    MIRROR,
}

data class LinearGradient(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val stops: List<GradientStop>,
    val tileMode: TileMode = TileMode.CLAMP,
) : Paint

data class RadialGradient(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val stops: List<GradientStop>,
    val tileMode: TileMode = TileMode.CLAMP,
) : Paint

data class SweepGradient(
    val centerX: Float,
    val centerY: Float,
    val stops: List<GradientStop>,
) : Paint
