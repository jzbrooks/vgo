package com.jzbrooks.vgo.core.util.math

import kotlin.math.abs

data class LineSegment(val first: Point, val second: Point) {
    fun intersection(other: LineSegment, tolerance: Float = 1e-3f): Point? {
        // this represented as a1x + b1y = c1
        val a1 = this.second.y - this.first.y
        val b1 = this.first.x - this.second.x
        val c1 = a1*this.first.x + b1*this.first.y

        // second represented as a1x + b1y = c1
        val a2 = other.second.y - other.first.y
        val b2 = other.first.x - other.second.x
        val c2 = a2*other.first.x + b2*other.first.y

        val determinant = a1*b2 - a2*b1
        if (abs(determinant) <= tolerance) {
            return null
        }

        val x = (b2*c1 - b1*c2) / determinant
        val y = (a1*c2 - a2*c1) / determinant

        return Point(x, y)
    }
}