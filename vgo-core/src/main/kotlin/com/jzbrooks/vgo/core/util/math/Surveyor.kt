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
    private val pathStart = ArrayDeque<Point>()

    // Updated once per process call when computing
    // the other variant of the command. This works
    // because the coordinates are accurate regardless
    // of their absolute or relative nature.
    private lateinit var currentPoint: Point
    private lateinit var previousControlPoint: Point
    private lateinit var rectangle: Rectangle

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

    fun findBoundingBox(commands: List<Command>): Rectangle {
        pathStart.clear()
        currentPoint = Point(0f, 0f)

        (commands.firstOrNull() as MoveTo?)?.let { firstMoveTo ->
            currentPoint = firstMoveTo.parameters.first()
            previousControlPoint = currentPoint
            pathStart.addFirst(currentPoint.copy())
            rectangle = Rectangle(currentPoint.x, currentPoint.y, currentPoint.x, currentPoint.y)

            updateBoundingBoxForLineParameters(firstMoveTo.variant, firstMoveTo.parameters.drop(1))
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
                    if (command.variant == CommandVariant.RELATIVE) {
                        currentPoint += command.parameters.first()
                    } else {
                        currentPoint = command.parameters.first()
                    }

                    pathStart.addFirst(currentPoint.copy())

                    updateBoundingBoxForLineParameters(command.variant, command.parameters.drop(1))
                }

                is LineTo -> {
                    updateBoundingBoxForLineParameters(command.variant, command.parameters)
                }

                is HorizontalLineTo -> {
                    for (parameter in command.parameters) {
                        currentPoint =
                            if (command.variant == CommandVariant.RELATIVE) {
                                currentPoint.copy(x = currentPoint.x + parameter)
                            } else {
                                currentPoint.copy(x = parameter)
                            }

                        if (currentPoint.x < rectangle.left) {
                            rectangle = rectangle.copy(left = currentPoint.x)
                        }

                        if (currentPoint.x > rectangle.right) {
                            rectangle = rectangle.copy(right = currentPoint.x)
                        }
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

                        if (currentPoint.y > rectangle.top) {
                            rectangle = rectangle.copy(top = currentPoint.y)
                        }

                        if (currentPoint.y < rectangle.bottom) {
                            rectangle = rectangle.copy(bottom = currentPoint.y)
                        }
                    }
                }

                is CubicBezierCurve -> {
                    for (parameter in command.parameters) {
                        if (command.variant == CommandVariant.RELATIVE) {
                            for (t in interpolationResolution) {
                                val interpolatedPoint = parameter.interpolate(Point.ZERO, t) + currentPoint
                                expandBoundingBoxForPoint(interpolatedPoint)
                            }

                            previousControlPoint = currentPoint + parameter.endControl
                            currentPoint += parameter.end
                        } else {
                            for (t in interpolationResolution) {
                                val interpolatedPoint = parameter.interpolate(currentPoint, t)
                                expandBoundingBoxForPoint(interpolatedPoint)
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
                                val interpolatedPoint =
                                    parameter.interpolate(Point.ZERO, previousControlPoint - currentPoint, t) + currentPoint
                                expandBoundingBoxForPoint(interpolatedPoint)
                            }

                            previousControlPoint = currentPoint + parameter.endControl
                            currentPoint += parameter.end
                        } else {
                            for (t in interpolationResolution) {
                                val interpolatedPoint = parameter.interpolate(currentPoint, previousControlPoint, t)
                                expandBoundingBoxForPoint(interpolatedPoint)
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
                                val interpolatedPoint = parameter.interpolate(Point.ZERO, t) + currentPoint
                                expandBoundingBoxForPoint(interpolatedPoint)
                            }

                            previousControlPoint = currentPoint + parameter.control
                            currentPoint += parameter.end
                        } else {
                            for (t in interpolationResolution) {
                                val interpolatedPoint = parameter.interpolate(currentPoint, t)
                                expandBoundingBoxForPoint(interpolatedPoint)
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
                                val interpolatedPoint =
                                    parameter.interpolateSmoothQuadraticBezierCurve(Point.ZERO, control - currentPoint, t) + currentPoint
                                expandBoundingBoxForPoint(interpolatedPoint)
                            }

                            previousControlPoint = control
                            currentPoint += parameter
                        } else {
                            for (t in interpolationResolution) {
                                val interpolatedPoint = parameter.interpolateSmoothQuadraticBezierCurve(currentPoint, control, t)
                                expandBoundingBoxForPoint(interpolatedPoint)
                            }

                            previousControlPoint = control
                            currentPoint = parameter
                        }
                    }
                }

                is EllipticalArcCurve -> {
                    for (arcParameter in command.parameters) {
                        val box = arcParameter.computeBoundingBox(command.variant, currentPoint)

                        expandBoundingBoxForBox(box)

                        if (command.variant == CommandVariant.RELATIVE) {
                            currentPoint += arcParameter.end
                        } else {
                            currentPoint = arcParameter.end
                        }
                    }
                }

                is ClosePath -> {
                    // If there is a close path, there should be a corresponding path start entry on the stack
                    currentPoint = pathStart.removeFirst()
                    command
                }
            }
        }

        return rectangle
    }

    private fun updateBoundingBoxForLineParameters(
        variant: CommandVariant,
        linesTo: List<Point>,
    ) {
        for (lineToParameter in linesTo) {
            if (variant == CommandVariant.RELATIVE) {
                currentPoint += lineToParameter
            } else {
                currentPoint = lineToParameter
            }

            expandBoundingBoxForPoint(currentPoint)
        }
    }

    private fun expandBoundingBoxForPoint(point: Point) {
        if (point.x < rectangle.left) {
            rectangle = rectangle.copy(left = point.x)
        }

        if (point.x > rectangle.right) {
            rectangle = rectangle.copy(right = point.x)
        }

        if (point.y > rectangle.top) {
            rectangle = rectangle.copy(top = point.y)
        }

        if (point.y < rectangle.bottom) {
            rectangle = rectangle.copy(bottom = point.y)
        }
    }

    private fun expandBoundingBoxForBox(box: Rectangle) {
        if (box.left < rectangle.left) {
            rectangle = rectangle.copy(left = box.left)
        }

        if (box.right > rectangle.right) {
            rectangle = rectangle.copy(right = box.right)
        }

        if (box.top > rectangle.top) {
            rectangle = rectangle.copy(top = box.top)
        }

        if (box.bottom < rectangle.bottom) {
            rectangle = rectangle.copy(bottom = box.bottom)
        }
    }

    private companion object {
        const val RESOLUTION = 10
        val interpolationResolution = (1..RESOLUTION).map { it / RESOLUTION.toDouble() }.map(Double::toFloat)
    }
}
