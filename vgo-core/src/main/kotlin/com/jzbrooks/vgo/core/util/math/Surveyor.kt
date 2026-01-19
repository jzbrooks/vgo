package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo

/**
 * Makes determinations based on the boundaries of a given set of path data
 */
class Surveyor {
    private val Command.shouldResetPreviousControlPoint: Boolean
        get() =
            when (this) {
                is MoveTo,
                is LineTo,
                is HorizontalLineTo,
                is VerticalLineTo,
                is EllipticalArcCurve,
                ClosePath,
                -> true

                is CubicBezierCurve,
                is SmoothCubicBezierCurve,
                is QuadraticBezierCurve,
                is SmoothQuadraticBezierCurve,
                -> false
            }

    /**
     * Samples points along the path commands for use in collision detection.
     * Returns the convex hull of all sampled points.
     */
    fun sampleConvexHull(commands: List<Command>): List<Point> {
        val sampledPoints = traversePoints(commands).toList()
        return convexHull(sampledPoints)
    }

    fun findBoundingBox(commands: List<Command>): Rectangle {
        val iter = traversePoints(commands).iterator()
        if (!iter.hasNext()) return Rectangle(0f, 0f, 0f, 0f)

        val first = iter.next()
        var box = Rectangle(first.x, first.y, first.x, first.y)
        while (iter.hasNext()) {
            val point = iter.next()
            box =
                Rectangle(
                    left = minOf(box.left, point.x),
                    top = maxOf(box.top, point.y),
                    right = maxOf(box.right, point.x),
                    bottom = minOf(box.bottom, point.y),
                )
        }
        return box
    }

    private fun traversePoints(commands: List<Command>): Sequence<Point> =
        sequence {
            val pathStart = ArrayDeque<Point>()
            var currentPoint = Point(0f, 0f)
            var previousControlPoint = currentPoint

            val firstMoveTo = commands.firstOrNull() as? MoveTo ?: return@sequence
            currentPoint = firstMoveTo.parameters.first()
            previousControlPoint = currentPoint
            pathStart.addFirst(currentPoint.copy())
            yield(currentPoint)

            for (param in firstMoveTo.parameters.drop(1)) {
                currentPoint =
                    if (firstMoveTo.variant == CommandVariant.RELATIVE) {
                        currentPoint + param
                    } else {
                        param
                    }
                yield(currentPoint)
            }

            for (i in commands.indices.drop(1)) {
                val command = commands[i]
                val previousCommand = commands.getOrNull(i - 1)

                if (previousCommand is ClosePath && command !is MoveTo) {
                    pathStart.addFirst(currentPoint.copy())
                }

                if (command.shouldResetPreviousControlPoint) {
                    previousControlPoint = currentPoint
                }

                when (command) {
                    is MoveTo -> {
                        currentPoint =
                            if (command.variant == CommandVariant.RELATIVE) {
                                currentPoint + command.parameters.first()
                            } else {
                                command.parameters.first()
                            }
                        yield(currentPoint)
                        pathStart.addFirst(currentPoint.copy())

                        for (param in command.parameters.drop(1)) {
                            currentPoint =
                                if (command.variant == CommandVariant.RELATIVE) {
                                    currentPoint + param
                                } else {
                                    param
                                }
                            yield(currentPoint)
                        }
                    }

                    is LineTo -> {
                        for (param in command.parameters) {
                            currentPoint =
                                if (command.variant == CommandVariant.RELATIVE) {
                                    currentPoint + param
                                } else {
                                    param
                                }
                            yield(currentPoint)
                        }
                    }

                    is HorizontalLineTo -> {
                        for (parameter in command.parameters) {
                            currentPoint =
                                if (command.variant == CommandVariant.RELATIVE) {
                                    currentPoint.copy(x = currentPoint.x + parameter)
                                } else {
                                    currentPoint.copy(x = parameter)
                                }
                            yield(currentPoint)
                        }
                    }

                    is VerticalLineTo -> {
                        for (parameter in command.parameters) {
                            currentPoint =
                                if (command.variant == CommandVariant.RELATIVE) {
                                    currentPoint.copy(y = currentPoint.y + parameter)
                                } else {
                                    currentPoint.copy(y = parameter)
                                }
                            yield(currentPoint)
                        }
                    }

                    is CubicBezierCurve -> {
                        for (parameter in command.parameters) {
                            if (command.variant == CommandVariant.RELATIVE) {
                                for (t in interpolationResolution) {
                                    yield(parameter.interpolate(Point.ZERO, t) + currentPoint)
                                }
                                previousControlPoint = currentPoint + parameter.endControl
                                currentPoint += parameter.end
                            } else {
                                for (t in interpolationResolution) {
                                    yield(parameter.interpolate(currentPoint, t))
                                }
                                previousControlPoint = parameter.endControl
                                currentPoint = parameter.end
                            }
                        }
                    }

                    is SmoothCubicBezierCurve -> {
                        for (parameter in command.parameters) {
                            if (command.variant == CommandVariant.RELATIVE) {
                                for (t in interpolationResolution) {
                                    yield(parameter.interpolate(Point.ZERO, previousControlPoint - currentPoint, t) + currentPoint)
                                }
                                previousControlPoint = currentPoint + parameter.endControl
                                currentPoint += parameter.end
                            } else {
                                for (t in interpolationResolution) {
                                    yield(parameter.interpolate(currentPoint, previousControlPoint, t))
                                }
                                previousControlPoint = parameter.endControl
                                currentPoint = parameter.end
                            }
                        }
                    }

                    is QuadraticBezierCurve -> {
                        for (parameter in command.parameters) {
                            if (command.variant == CommandVariant.RELATIVE) {
                                for (t in interpolationResolution) {
                                    yield(parameter.interpolate(Point.ZERO, t) + currentPoint)
                                }
                                previousControlPoint = currentPoint + parameter.control
                                currentPoint += parameter.end
                            } else {
                                for (t in interpolationResolution) {
                                    yield(parameter.interpolate(currentPoint, t))
                                }
                                previousControlPoint = parameter.control
                                currentPoint = parameter.end
                            }
                        }
                    }

                    is SmoothQuadraticBezierCurve -> {
                        for (parameter in command.parameters) {
                            val control = currentPoint * 2f - previousControlPoint

                            if (command.variant == CommandVariant.RELATIVE) {
                                for (t in interpolationResolution) {
                                    yield(
                                        parameter.interpolateSmoothQuadraticBezierCurve(Point.ZERO, control - currentPoint, t) +
                                            currentPoint,
                                    )
                                }
                                previousControlPoint = control
                                currentPoint += parameter
                            } else {
                                for (t in interpolationResolution) {
                                    yield(parameter.interpolateSmoothQuadraticBezierCurve(currentPoint, control, t))
                                }
                                previousControlPoint = control
                                currentPoint = parameter
                            }
                        }
                    }

                    is EllipticalArcCurve -> {
                        for (arcParameter in command.parameters) {
                            val box = arcParameter.computeBoundingBox(command.variant, currentPoint)
                            yield(Point(box.left, box.top))
                            yield(Point(box.right, box.top))
                            yield(Point(box.right, box.bottom))
                            yield(Point(box.left, box.bottom))

                            currentPoint =
                                if (command.variant == CommandVariant.RELATIVE) {
                                    currentPoint + arcParameter.end
                                } else {
                                    arcParameter.end
                                }
                        }
                    }

                    is ClosePath -> {
                        currentPoint = pathStart.removeFirst()
                    }
                }
            }
        }

    private companion object {
        private const val RESOLUTION = 10
        val interpolationResolution = (1..RESOLUTION).map { it / RESOLUTION.toDouble() }.map(Double::toFloat)
    }
}
