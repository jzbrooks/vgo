package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.Gradient
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.SweepGradient
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

private const val RELATIVE_EPSILON = 1e-4f

/**
 * Maps the gradient's coordinates through [matrix], or returns null when the
 * transformed gradient is not exactly representable by the same gradient type
 * (gradients carry no transform of their own, so the transform must be baked
 * into their coordinates without changing how they render).
 */
fun Gradient.transformedOrNull(matrix: Matrix3): Gradient? =
    when (this) {
        is LinearGradient -> transformedOrNull(matrix)
        is RadialGradient -> transformedOrNull(matrix)
        is SweepGradient -> transformedOrNull(matrix)
    }

/**
 * A linear gradient's stop lines run perpendicular to its start→end vector.
 * Mapping the endpoints through the matrix is only faithful if the transformed
 * stop lines remain perpendicular to the transformed gradient vector — true for
 * all similarity transforms and for axis-aligned gradients under axis scaling.
 */
fun LinearGradient.transformedOrNull(matrix: Matrix3): LinearGradient? {
    val vX = endX - startX
    val vY = endY - startY
    if (vX == 0f && vY == 0f) return null

    val mappedVX = matrix[0, 0] * vX + matrix[0, 1] * vY
    val mappedVY = matrix[1, 0] * vX + matrix[1, 1] * vY
    // The normal of the gradient vector, mapped through the matrix
    val mappedNX = matrix[0, 0] * -vY + matrix[0, 1] * vX
    val mappedNY = matrix[1, 0] * -vY + matrix[1, 1] * vX

    val mappedVLength = hypot(mappedVX, mappedVY)
    val mappedNLength = hypot(mappedNX, mappedNY)
    if (mappedVLength == 0f || mappedNLength == 0f) return null

    val dot = mappedVX * mappedNX + mappedVY * mappedNY
    if (abs(dot) > RELATIVE_EPSILON * mappedVLength * mappedNLength) return null

    val start = matrix * Vector3(Point(startX, startY))
    val end = matrix * Vector3(Point(endX, endY))
    return copy(
        startX = start.i,
        startY = start.j,
        endX = end.i,
        endY = end.j,
    )
}

/**
 * A radial gradient's stop lines are circles, which only map to circles under
 * uniform scale, rotation, reflection, and translation.
 */
fun RadialGradient.transformedOrNull(matrix: Matrix3): RadialGradient? {
    val uX = matrix[0, 0]
    val uY = matrix[1, 0]
    val wX = matrix[0, 1]
    val wY = matrix[1, 1]

    val uLength = hypot(uX, uY)
    val wLength = hypot(wX, wY)
    if (uLength == 0f || wLength == 0f) return null

    val dot = uX * wX + uY * wY
    if (abs(dot) > RELATIVE_EPSILON * uLength * wLength) return null
    if (abs(uLength - wLength) > RELATIVE_EPSILON * max(uLength, wLength)) return null

    val center = matrix * Vector3(Point(centerX, centerY))
    return copy(
        centerX = center.i,
        centerY = center.j,
        radius = radius * uLength,
    )
}

/**
 * A sweep gradient's start angle is fixed, so rotation and reflection would
 * shift its stops. Only uniform positive scale and translation are faithful.
 */
fun SweepGradient.transformedOrNull(matrix: Matrix3): SweepGradient? {
    val scale = matrix[0, 0]
    if (scale <= 0f) return null

    val uniformScale =
        abs(matrix[1, 1] - scale) <= RELATIVE_EPSILON * scale &&
            abs(matrix[0, 1]) <= RELATIVE_EPSILON * scale &&
            abs(matrix[1, 0]) <= RELATIVE_EPSILON * scale
    if (!uniformScale) return null

    val center = matrix * Vector3(Point(centerX, centerY))
    return copy(
        centerX = center.i,
        centerY = center.j,
    )
}
