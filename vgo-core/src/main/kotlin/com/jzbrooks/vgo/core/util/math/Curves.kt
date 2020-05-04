package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.*
import kotlin.math.abs
import kotlin.math.min

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
 */
fun CubicBezierCurve.fitCircle(tolerance: Float = 0.01f): Circle? {
    check(variant == CommandVariant.RELATIVE)
    check(parameters.size == 1)

    val (_, _, end) = parameters[0]

    val mid = interpolate(0.5f)
    val m1 = mid * 0.5f
    val m2 = (mid + end) * 0.5f

    val firstDiagonal = LineSegment(m1, Point(m1.x + m1.y, m1.y - m1.x))
    val secondDiagonal = LineSegment(m2, Point(m2.x + (m2.y - mid.y), m2.y - (m2.x - mid.x)))
    val center = firstDiagonal.intersection(secondDiagonal) ?: return null
    val radius = Point.zero.distanceTo(center)

    // Do we need to parameterize this?
    @Suppress("NAME_SHADOWING")
    val tolerance = min(2.5f * tolerance, 0.5f * radius / 100f)

    val withinTolerance = floatArrayOf(1/4f, 3/4f).all {
        val curveValue = interpolate(it)
        abs(curveValue.distanceTo(center) - radius) <= tolerance
    }

    return if (withinTolerance) Circle(center, radius) else null
}

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
 */
fun ShortcutCubicBezierCurve.fitCircle(tolerance: Float = 0.01f): Circle? {
    check(variant == CommandVariant.RELATIVE)
    check(parameters.size == 1)

    val (_, end) = parameters[0]

    val mid = interpolate(0.5f)
    val m1 = mid * 0.5f
    val m2 = (mid + end) * 0.5f

    val firstDiagonal = LineSegment(m1, Point(m1.x + m1.y, m1.y - m1.x))
    val secondDiagonal = LineSegment(m2, Point(m2.x + (m2.y - mid.y), m2.y - (m2.x - mid.x)))
    val center = firstDiagonal.intersection(secondDiagonal) ?: return null
    val radius = Point.zero.distanceTo(center)

    // Do we need to parameterize this?
    @Suppress("NAME_SHADOWING")
    val tolerance = min(2.5f * tolerance, 0.5f * radius / 100f)

    val withinTolerance = floatArrayOf(1/4f, 3/4f).all {
        val curveValue = interpolate(it)
        abs(curveValue.distanceTo(center) - radius) <= tolerance
    }

    return if (withinTolerance) Circle(center, radius) else null
}

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
 */
fun CubicBezierCurve.interpolate(t: Float): Point {
    check(variant == CommandVariant.RELATIVE)
    check(parameters.size == 1)

    val (startControl, endControl, end) = parameters[0]

    val square = t * t
    val cube = square * t
    val param = 1 - t
    val paramSquare = param * param

    return Point(
            3 * paramSquare * t * startControl.x + 3 * param * square * endControl.x + cube * end.x,
            3 * paramSquare * t * startControl.y + 3 * param * square * endControl.y + cube * end.y
    )
}

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
 */
fun ShortcutCubicBezierCurve.interpolate(t: Float): Point {
    check(variant == CommandVariant.RELATIVE)
    check(parameters.size == 1)

    val (control, end) = parameters[0]

    val square = t * t
    val cube = square * t
    val param = 1 - t

    return Point(
            3 * param * square * control.x + cube * end.x,
            3 * param * square * control.y + cube * end.y
    )
}

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
*/
fun CubicBezierCurve.isConvex(): Boolean {
    check(variant == CommandVariant.RELATIVE)
    check(parameters.size == 1)

    val (startControl, endControl, end) = parameters[0]

    val firstDiagonal = LineSegment(Point.zero, endControl)
    val secondDiagonal = LineSegment(startControl, end)

    val intersection = secondDiagonal.intersection(firstDiagonal)

    return intersection != null &&
            endControl.x < intersection.x == intersection.x < 0 &&
            endControl.y < intersection.y == intersection.y < 0 &&
            end.x < intersection.x == intersection.x < startControl.x &&
            end.y < intersection.y == intersection.y < startControl.y
}

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
 */
fun ShortcutCubicBezierCurve.isConvex(): Boolean {
    check(variant == CommandVariant.RELATIVE)
    check(parameters.size == 1)

    val (control, end) = parameters[0]

    val firstDiagonal = LineSegment(Point.zero, control)
    val secondDiagonal = LineSegment(Point.zero, end)

    val intersection = firstDiagonal.intersection(secondDiagonal)

    return intersection != null &&
            control.x < intersection.x == intersection.x < 0 &&
            control.y < intersection.y == intersection.y < 0 &&
            end.x < intersection.x == intersection.x < 0 &&
            end.y < intersection.y == intersection.y < 0
}