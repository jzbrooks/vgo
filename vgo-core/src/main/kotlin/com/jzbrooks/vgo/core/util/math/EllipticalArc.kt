package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val EPSILON = 1e-6f

/**
 * Apply the linear portion of [transform] to this arc's radii, x-axis-rotation, and sweep
 * flag. The endpoint is left untouched — callers transform it separately.
 *
 * Treats the arc as part of an ellipse defined by `M = R(angle) · diag(radiusX, radiusY)`.
 * Under a linear transform `L`, the new ellipse matrix is `L · M`; the SVD of that matrix
 * (computed via the symmetric `S = L·M · (L·M)ᵀ`) yields the new radii and rotation.
 */
fun EllipticalArcCurve.Parameter.applyTransform(transform: Matrix3) {
    val l00 = transform[0, 0]
    val l01 = transform[0, 1]
    val l10 = transform[1, 0]
    val l11 = transform[1, 1]

    val radians = angle * PI.toFloat() / 180f
    val cosA = cos(radians)
    val sinA = sin(radians)

    // M' = L · R(angle) · diag(radiusX, radiusY)
    val m00 = (l00 * cosA + l01 * sinA) * radiusX
    val m10 = (l10 * cosA + l11 * sinA) * radiusX
    val m01 = (-l00 * sinA + l01 * cosA) * radiusY
    val m11 = (-l10 * sinA + l11 * cosA) * radiusY

    // S = M' · M'ᵀ (2x2 symmetric)
    val s00 = m00 * m00 + m01 * m01
    val s01 = m00 * m10 + m01 * m11
    val s11 = m10 * m10 + m11 * m11

    val trace = s00 + s11
    val diff = s00 - s11
    val discriminant = sqrt(diff * diff + 4f * s01 * s01)
    val lambda1 = max(0f, (trace + discriminant) / 2f)
    val lambda2 = max(0f, (trace - discriminant) / 2f)

    radiusX = sqrt(lambda1)
    radiusY = sqrt(lambda2)

    angle =
        if (abs(s01) < EPSILON) {
            if (s00 >= s11) 0f else 90f
        } else {
            atan2(lambda1 - s00, s01) * 180f / PI.toFloat()
        }

    val det = l00 * l11 - l01 * l10
    if (det < 0f) {
        sweep =
            when (sweep) {
                EllipticalArcCurve.SweepFlag.CLOCKWISE -> EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
                EllipticalArcCurve.SweepFlag.ANTICLOCKWISE -> EllipticalArcCurve.SweepFlag.CLOCKWISE
            }
    }
}
