package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.CubicCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.min

private const val ARC_THRESHOLD = 2f
private const val ARC_TOLERANCE = 0.5f

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
 */
fun CubicBezierCurve.fitCircle(tolerance: Float = 1e-3f): Circle? {
    assert(variant == CommandVariant.RELATIVE)
    assert(parameters.size == 1)

    val mid = interpolateRelative(0.5f)

    val end = parameters[0].end
    val m1 = mid * 0.5f
    val m2 = (mid + end) * 0.5f

    val firstDiagonal = LineSegment(m1, Point(m1.x + m1.y, m1.y - m1.x))
    val secondDiagonal = LineSegment(m2, Point(m2.x + (m2.y - mid.y), m2.y - (m2.x - mid.x)))
    val center = firstDiagonal.intersection(secondDiagonal) ?: return null
    val radius = Point.ZERO.distanceTo(center)

    // Do we need to parameterize this?
    @Suppress("NAME_SHADOWING")
    val tolerance = ARC_THRESHOLD * tolerance

    val withinTolerance =
        radius < 1e7 &&
            floatArrayOf(0.25f, 0.75f).all {
                val curveValue = interpolateRelative(it)
                abs(curveValue.distanceTo(center) - radius) <= tolerance
            }

    return if (withinTolerance) Circle(center, radius) else null
}

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
 */
fun CubicBezierCurve.interpolateRelative(t: Float): Point {
    assert(variant == CommandVariant.RELATIVE)
    return interpolate(Point.ZERO, t)
}

/**
 * Requires that the curve only has a single parameter
 */
fun CubicBezierCurve.interpolate(
    currentPoint: Point,
    t: Float,
): Point {
    assert(parameters.size == 1)
    return parameters.first().interpolate(currentPoint, t)
}

/**
 * Requires that the curve only has a single parameter
 */
fun CubicBezierCurve.Parameter.interpolate(
    currentPoint: Point,
    t: Float,
): Point {
    val square = t * t
    val cube = square * t
    val param = 1 - t
    val paramSquare = param * param
    val paramCube = paramSquare * param

    return Point(
        x = currentPoint.x * paramCube + 3 * paramSquare * t * startControl.x + 3 * param * square * endControl.x + cube * end.x,
        y = currentPoint.y * paramCube + 3 * paramSquare * t * startControl.y + 3 * param * square * endControl.y + cube * end.y,
    )
}

/**
 * Requires that the curve only has a single parameter
 * Requires that the curve use relative coordinates
*/
fun CubicBezierCurve.isConvex(tolerance: Float = 1e-3f): Boolean {
    assert(variant == CommandVariant.RELATIVE)
    assert(parameters.size == 1)

    val (startControl, endControl, end) = parameters[0]

    val firstDiagonal = LineSegment(Point.ZERO, endControl)
    val secondDiagonal = LineSegment(startControl, end)

    val intersection = secondDiagonal.intersection(firstDiagonal, tolerance)

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
fun SmoothCubicBezierCurve.toCubicBezierCurve(previous: CubicCurve<*>): CubicBezierCurve {
    assert(variant == CommandVariant.RELATIVE)
    assert(previous.variant == CommandVariant.RELATIVE)
    assert(parameters.size == 1)
    assert(previous.parameters.size == 1)

    val (prevEndControl, prevEnd) =
        when (val previousParam = previous.parameters[0]) {
            is CubicBezierCurve.Parameter -> previousParam.endControl to previousParam.end
            is SmoothCubicBezierCurve.Parameter -> previousParam.endControl to previousParam.end
            else -> throw IllegalStateException("A destructuring of control points is required for ${previous::class.simpleName}.")
        }

    return CubicBezierCurve(
        variant,
        parameters.map { (endControl, end) ->
            CubicBezierCurve.Parameter(prevEnd - prevEndControl, endControl, end)
        },
    )
}

fun SmoothCubicBezierCurve.interpolateRelative(
    previousControl: Point,
    t: Float,
): Point {
    assert(variant == CommandVariant.RELATIVE)

    return interpolate(Point.ZERO, previousControl, t)
}

fun SmoothCubicBezierCurve.interpolate(
    currentPoint: Point,
    previousControl: Point,
    t: Float,
): Point {
    assert(parameters.size == 1)

    val startControl = (currentPoint * 2f) - previousControl

    val (endControl, end) = parameters[0]

    val param = 1 - t
    val paramSquare = param * param
    val paramCube = paramSquare * param
    val square = t * t
    val cube = square * t

    return Point(
        x = paramCube * currentPoint.x + 3 * paramSquare * t * startControl.x + 3 * param * square * endControl.x + cube * end.x,
        y = paramCube * currentPoint.y + 3 * paramSquare * t * startControl.y + 3 * param * square * endControl.y + cube * end.y,
    )
}

fun SmoothCubicBezierCurve.Parameter.interpolate(
    currentPoint: Point,
    previousControl: Point,
    t: Float,
): Point {
    val startControl = (currentPoint * 2f) - previousControl

    val param = 1 - t
    val paramSquare = param * param
    val paramCube = paramSquare * param
    val square = t * t
    val cube = square * t

    return Point(
        x = paramCube * currentPoint.x + 3 * paramSquare * t * startControl.x + 3 * param * square * endControl.x + cube * end.x,
        y = paramCube * currentPoint.y + 3 * paramSquare * t * startControl.y + 3 * param * square * endControl.y + cube * end.y,
    )
}

fun CubicBezierCurve.liesOnCircle(
    circle: Circle,
    tolerance: Float = 1e-3f,
): Boolean {
    @Suppress("NAME_SHADOWING")
    val tolerance = min(ARC_THRESHOLD * tolerance, ARC_TOLERANCE * circle.radius / 100)

    return floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f).all { t ->
        abs(interpolateRelative(t).distanceTo(circle.center) - circle.radius) <= tolerance
    }
}

fun CubicBezierCurve.findArcAngle(circle: Circle): Float {
    val center = circle.center
    val edge = parameters[0].end

    val innerProduct = center.x * edge.x + center.y * edge.y
    val magnitudeProduct = hypot(center.x, center.y) * hypot(edge.x, edge.y)

    return acos(innerProduct / magnitudeProduct)
}

fun QuadraticBezierCurve.interpolateRelative(t: Float): Point {
    assert(variant == CommandVariant.RELATIVE)
    return interpolate(Point.ZERO, t)
}

fun QuadraticBezierCurve.interpolate(
    currentPoint: Point,
    t: Float,
): Point {
    assert(parameters.size == 1)

    val (control, end) = parameters[0]

    val param = 1 - t
    val paramSquare = param * param
    val square = t * t

    return Point(
        x = paramSquare * currentPoint.x + 2 * param * t * control.x + square * end.x,
        y = paramSquare * currentPoint.y + 2 * param * t * control.y + square * end.y,
    )
}

fun SmoothQuadraticBezierCurve.interpolateRelative(
    previousControl: Point,
    t: Float,
): Point {
    assert(variant == CommandVariant.RELATIVE)
    return interpolate(Point.ZERO, previousControl, t)
}

fun SmoothQuadraticBezierCurve.interpolate(
    currentPoint: Point,
    previousControl: Point,
    t: Float,
): Point {
    assert(parameters.size == 1)

    val end = parameters[0]

    // Calculate the "smooth" control point by reflecting `previousControl` across `currentPoint`
    val control = (currentPoint * 2f) - previousControl

    val param = 1 - t
    val paramSquare = param * param
    val square = t * t

    return Point(
        x = paramSquare * currentPoint.x + 2 * param * t * control.x + square * end.x,
        y = paramSquare * currentPoint.y + 2 * param * t * control.y + square * end.y,
    )
}

fun EllipticalArcCurve.computeBoundingBox(currentPoint: Point): Rectangle {
    assert(parameters.size == 1)

    val rx = parameters[0].radiusX
    val ry = parameters[0].radiusY
    val endPoint = parameters[0].end

    return Rectangle(
        left = minOf(currentPoint.x - rx, endPoint.x - rx),
        right = maxOf(currentPoint.x + rx, endPoint.x + rx),
        top = maxOf(currentPoint.y + ry, endPoint.y + ry),
        bottom = minOf(currentPoint.y - ry, endPoint.y - ry),
    )
}
