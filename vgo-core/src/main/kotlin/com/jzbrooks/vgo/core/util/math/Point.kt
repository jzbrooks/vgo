package com.jzbrooks.vgo.core.util.math

data class Point(var x: Float, var y: Float) {
    operator fun plus(other: Point): Point {
        return Point(x + other.x, y + other.y)
    }

    operator fun minus(other: Point): Point {
        return Point(x - other.x, y - other.y)
    }

    operator fun times(other: Point): Point {
        return Point(x * other.x, y * other.y)
    }

    companion object {
        val zero = Point(0f, 0f)
    }
}
