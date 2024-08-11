 package com.jzbrooks.vgo.core.util.math

import kotlin.math.absoluteValue

data class LineSegment(val first: Point, val second: Point) {
    fun intersection(
        other: LineSegment,
        tolerance: Float = 1e-3f,
    ): Point? {
        // this represented as a1x + b1y = c1
        val a1 = second.y - first.y
        val b1 = first.x - second.x
        val c1 = a1 * first.x + b1 * first.y

        // second represented as a2x + b2y = c2
        val a2 = other.second.y - other.first.y
        val b2 = other.first.x - other.second.x
        val c2 = a2 * other.first.x + b2 * other.first.y

        val determinant = a1 * b2 - a2 * b1
        if (determinant.absoluteValue <= tolerance) {
            return null
        }

        val x = (b2 * c1 - b1 * c2) / determinant
        val y = (a1 * c2 - a2 * c1) / determinant

        val intersectionPoint = Point(x, y)

        if (contains(intersectionPoint) && other.contains(intersectionPoint)) {
            return intersectionPoint
        }

        return null
    }

    // If the dot product is less than zero or greater than
    // the squared length of the segment, then the point is
    // collinear _but_ does not lie on the line segment
    //
    // Collinearity checks are skipped since we already know
    // the intersection point lies on the line segments
    // because they intersect each other
    private fun contains(point: Point): Boolean {
        val dot = (point.x - first.x) * (second.x - first.x) +
                (point.y - first.y) * (second.y - first.y)

        if (dot < 0) return false

        val squaredLength = (second.x - first.x) * (second.x - first.x) +
                (second.y - first.y) * (second.y - first.y)

        return dot <= squaredLength
    }
}
