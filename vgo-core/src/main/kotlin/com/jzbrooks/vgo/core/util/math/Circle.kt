package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve.ArcFlag
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve.SweepFlag
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Circle(
    var center: Point,
    val radius: Float,
)

fun EllipticalArcCurve.interpolate(currentPoint: Point, t: Float): Point {
    assert(parameters.size == 1)

    val (radiusX, radiusY, angle, arc, sweep, end) = parameters[0]

    val radAngle = Math.toRadians(angle.toDouble())

    val dx2 = (currentPoint.x - end.x) / 2.0
    val dy2 = (currentPoint.y - end.y) / 2.0

    val cosAngle = cos(radAngle)
    val sinAngle = sin(radAngle)

    val x1p = cosAngle * dx2 + sinAngle * dy2
    val y1p = -sinAngle * dx2 + cosAngle * dy2

    val rx = radiusX.toFloat().absoluteValue
    val ry = radiusY.toFloat().absoluteValue

    val rx2 = rx * rx
    val ry2 = ry * ry
    val x1p2 = x1p * x1p
    val y1p2 = y1p * y1p

    val radicant = ((rx2 * ry2) - (rx2 * y1p2) - (ry2 * x1p2)) /
            ((rx2 * y1p2) + (ry2 * x1p2))
    val factor = if (arc == ArcFlag.LARGE) sqrt(radicant.coerceAtLeast(0.0)) else -sqrt(radicant.coerceAtLeast(0.0))

    val cxp = factor * (rx * y1p / ry)
    val cyp = factor * (-ry * x1p / rx)

    val cx = cosAngle * cxp - sinAngle * cyp + (currentPoint.x + end.x) / 2.0
    val cy = sinAngle * cxp + cosAngle * cyp + (currentPoint.y + end.y) / 2.0

    val startAngle = atan2((y1p - cyp) / ry, (x1p - cxp) / rx)
    val deltaAngle = atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx) - startAngle

    val adjustedDeltaAngle = if (sweep == SweepFlag.CLOCKWISE) deltaAngle else -deltaAngle
    val normalizedDeltaAngle = (adjustedDeltaAngle + 2 * Math.PI) % (2 * Math.PI)

    val interpolatedAngle = startAngle + normalizedDeltaAngle * t

    val x = cx + rx * cos(interpolatedAngle) * cosAngle - ry * sin(interpolatedAngle) * sinAngle
    val y = cy + rx * cos(interpolatedAngle) * sinAngle + ry * sin(interpolatedAngle) * cosAngle

    return Point(x.toFloat(), y.toFloat())
}


