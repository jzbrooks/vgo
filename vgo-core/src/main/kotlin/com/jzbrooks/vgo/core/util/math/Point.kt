package com.jzbrooks.vgo.core.util.math

import kotlin.math.hypot

data class Point(val x: Float, val y: Float) {
    operator fun plus(other: Point): Point {
        return Point(x + other.x, y + other.y)
    }

    operator fun minus(other: Point): Point {
        return Point(x - other.x, y - other.y)
    }

    operator fun times(other: Point): Point {
        return Point(x * other.x, y * other.y)
    }

    operator fun times(scalar: Float): Point {
        return Point(x * scalar, y * scalar)
    }

    fun distanceTo(other: Point): Float {
        return hypot(x - other.x, y - other.y)
    }

    companion object {
        val ZERO = Point(0f, 0f)
    }
}
