package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.*
import kotlin.math.*

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

fun QuadraticBezierCurve.Parameter.interpolate(
    currentPoint: Point,
    t: Float,
): Point {
    val param = 1 - t
    val paramSquare = param * param
    val square = t * t

    return Point(
        x = paramSquare * currentPoint.x + 2 * param * t * control.x + square * end.x,
        y = paramSquare * currentPoint.y + 2 * param * t * control.y + square * end.y,
    )
}

// todo: it might be a little nicer if the T parameter was a value class around a point
fun Point.interpolateSmoothQuadraticBezierCurve(
    currentPoint: Point,
    control: Point,
    t: Float,
): Point {
    val param = 1 - t
    val paramSquare = param * param
    val square = t * t

    return Point(
        x = paramSquare * currentPoint.x + 2 * param * t * control.x + square * x,
        y = paramSquare * currentPoint.y + 2 * param * t * control.y + square * y,
    )
}

fun EllipticalArcCurve.Parameter.computeCenter(currentPoint: Point, endOverride: Point? = null): Point {
    val phi = Math.toRadians(angle.toDouble())
    val cosPhi = cos(phi)
    val sinPhi = sin(phi)
    var rx = radiusX
    var ry = radiusY

    val end = endOverride ?: end

    val x1prime = cosPhi * (currentPoint.x - end.x) / 2.0f + sinPhi * (currentPoint.y - end.y) / 2.0f
    val y1prime = -sinPhi * (currentPoint.x - end.x) / 2.0f + cosPhi * (currentPoint.y - end.y) / 2.0f

    // handle minuscule radii
    val x1primeSquared = x1prime * x1prime
    val y1primeSquared = y1prime * y1prime

    var radiusChecker = x1primeSquared / (radiusX * radiusX) + y1primeSquared / (radiusY * radiusY)
    if (radiusChecker > 1) {
        val root = sqrt(radiusChecker)
        rx *= root.toFloat()
        ry *= root.toFloat()
    }

    var rx2 = rx * rx
    var ry2 = ry * ry
    val sign = if ((arc == EllipticalArcCurve.ArcFlag.LARGE) == (sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE)) -1 else 1
    val sq = ((rx2 * ry2 - rx2 * y1primeSquared - ry2 * x1primeSquared) / (rx2 * y1primeSquared + ry2 * x1primeSquared))
    val c = sign * sqrt(sq.coerceAtLeast(0.0))

    val cxPrime = c * (rx * y1prime) / ry
    val cyPrime = c * (-ry * x1prime) / rx

    val cx = cosPhi * cxPrime - sinPhi * cyPrime + (currentPoint.x + end.x) / 2.0
    val cy = sinPhi * cxPrime + cosPhi * cyPrime + (currentPoint.y + end.y) / 2.0

    return Point(cx.toFloat(), cy.toFloat())
}

fun EllipticalArcCurve.Parameter.computeBoundingBoxRelative(dxy: Point): Rectangle {
    val currentPoint = dxy
    val end = dxy + end

    val center = computeCenter(currentPoint, end)

    var box = Rectangle(
        center.x - radiusX,
        center.y + radiusY,
        center.x + radiusX,
        center.y - radiusY,
    )

    if (end.x < box.left) box = box.copy(left = end.x)
    if (end.x > box.right) box = box.copy(right = end.x)
    if (end.y > box.top) box = box.copy(top = end.y)
    if (end.y < box.bottom) box = box.copy(bottom = end.y)

    return box
}

fun EllipticalArcCurve.Parameter.computeBoundingBox(currentPoint: Point): Rectangle {
    val center = computeCenter(currentPoint)

    return Rectangle(
        center.x - radiusX,
        center.y + radiusY,
        center.x + radiusX,
        center.y - radiusY,
    )
}
