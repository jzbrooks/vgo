package com.jzbrooks.vgo.core.util.math

import kotlin.math.absoluteValue
import kotlin.math.hypot

data class Point(
    val x: Float,
    val y: Float,
) {
    operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)

    operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)

    operator fun times(other: Point): Point = Point(x * other.x, y * other.y)

    operator fun times(scalar: Float): Point = Point(x * scalar, y * scalar)

    fun distanceTo(other: Point): Float = hypot(x - other.x, y - other.y)

    fun isApproximately(
        other: Point,
        error: Float = 0.001f,
    ): Boolean = (x - other.x).absoluteValue < error && (y - other.y).absoluteValue < error

    companion object {
        val ZERO = Point(0f, 0f)
    }
}
