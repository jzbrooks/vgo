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

// TODO:
//   1. Cubic works - yes
//   2. Quad works - yes
//   3. Smooth quad - yes
//   4. Smooth cubic - yes
//   5. Arc - 1/2
//   6. Ensure point tracking is correct for every command type - yes
//   7. This needs to work with poly-commands (where commands have more than one set of parameters) - yes
//   8. What's the right resolution for interpolation?
//   9. Can shorthand curves exist directly after commands that aren't the corresponding longahand commands?
//      > If the S command doesn't follow another S or C command, then the current position of the cursor is used as the first control point.
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

            // TODO: is this right?
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
                        if (command.variant == CommandVariant.RELATIVE) {
                            currentPoint = currentPoint.copy(x = currentPoint.x + parameter)
                        } else {
                            currentPoint = currentPoint.copy(x = parameter)
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
                        if (command.variant == CommandVariant.RELATIVE) {
                            currentPoint = currentPoint.copy(y = currentPoint.y + parameter)
                        } else {
                            currentPoint = currentPoint.copy(y = parameter)
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
                            for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                                val interpolatedPoint = parameter.interpolate(Point.ZERO, t) + currentPoint
                                if (interpolatedPoint.x < rectangle.left) {
                                    rectangle = rectangle.copy(left = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.x > rectangle.right) {
                                    rectangle = rectangle.copy(right = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.y > rectangle.top) {
                                    rectangle = rectangle.copy(top = interpolatedPoint.y)
                                }

                                if (interpolatedPoint.y < rectangle.bottom) {
                                    rectangle = rectangle.copy(bottom = interpolatedPoint.y)
                                }
                            }

                            previousControlPoint = currentPoint + parameter.endControl
                            currentPoint += parameter.end
                        } else {
                            for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                                val interpolatedPoint = parameter.interpolate(currentPoint, t)
                                if (interpolatedPoint.x < rectangle.left) {
                                    rectangle = rectangle.copy(left = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.x > rectangle.right) {
                                    rectangle = rectangle.copy(right = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.y > rectangle.top) {
                                    rectangle = rectangle.copy(top = interpolatedPoint.y)
                                }

                                if (interpolatedPoint.y < rectangle.bottom) {
                                    rectangle = rectangle.copy(bottom = interpolatedPoint.y)
                                }
                            }

                            previousControlPoint = parameter.endControl
                            currentPoint = parameter.end
                        }
                    }
                }

                is SmoothCubicBezierCurve -> {
                    for (parameter in command.parameters) {
                        if (command.variant == CommandVariant.RELATIVE) {
                            for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                                val interpolatedPoint =
                                    parameter.interpolate(Point.ZERO, previousControlPoint - currentPoint, t) + currentPoint
                                if (interpolatedPoint.x < rectangle.left) {
                                    rectangle = rectangle.copy(left = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.x > rectangle.right) {
                                    rectangle = rectangle.copy(right = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.y > rectangle.top) {
                                    rectangle = rectangle.copy(top = interpolatedPoint.y)
                                }

                                if (interpolatedPoint.y < rectangle.bottom) {
                                    rectangle = rectangle.copy(bottom = interpolatedPoint.y)
                                }
                            }

                            previousControlPoint = currentPoint + parameter.endControl
                            currentPoint += parameter.end
                        } else {
                            for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                                val interpolatedPoint = parameter.interpolate(currentPoint, previousControlPoint, t)
                                if (interpolatedPoint.x < rectangle.left) {
                                    rectangle = rectangle.copy(left = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.x > rectangle.right) {
                                    rectangle = rectangle.copy(right = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.y > rectangle.top) {
                                    rectangle = rectangle.copy(top = interpolatedPoint.y)
                                }

                                if (interpolatedPoint.y < rectangle.bottom) {
                                    rectangle = rectangle.copy(bottom = interpolatedPoint.y)
                                }
                            }

                            previousControlPoint = parameter.endControl
                            currentPoint = parameter.end
                        }
                    }
                }

                is QuadraticBezierCurve -> {
                    for (parameter in command.parameters) {
                        if (command.variant == CommandVariant.RELATIVE) {
                            for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                                val interpolatedPoint = parameter.interpolate(Point.ZERO, t) + currentPoint
                                if (interpolatedPoint.x < rectangle.left) {
                                    rectangle = rectangle.copy(left = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.x > rectangle.right) {
                                    rectangle = rectangle.copy(right = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.y > rectangle.top) {
                                    rectangle = rectangle.copy(top = interpolatedPoint.y)
                                }

                                if (interpolatedPoint.y < rectangle.bottom) {
                                    rectangle = rectangle.copy(bottom = interpolatedPoint.y)
                                }
                            }

                            previousControlPoint = currentPoint + parameter.control
                            currentPoint += parameter.end
                        } else {
                            for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                                val interpolatedPoint = parameter.interpolate(currentPoint, t)
                                if (interpolatedPoint.x < rectangle.left) {
                                    rectangle = rectangle.copy(left = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.x > rectangle.right) {
                                    rectangle = rectangle.copy(right = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.y > rectangle.top) {
                                    rectangle = rectangle.copy(top = interpolatedPoint.y)
                                }

                                if (interpolatedPoint.y < rectangle.bottom) {
                                    rectangle = rectangle.copy(bottom = interpolatedPoint.y)
                                }
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
                            for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                                val interpolatedPoint =
                                    parameter.interpolateSmoothQuadraticBezierCurve(Point.ZERO, control - currentPoint, t) + currentPoint
                                if (interpolatedPoint.x < rectangle.left) {
                                    rectangle = rectangle.copy(left = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.x > rectangle.right) {
                                    rectangle = rectangle.copy(right = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.y > rectangle.top) {
                                    rectangle = rectangle.copy(top = interpolatedPoint.y)
                                }

                                if (interpolatedPoint.y < rectangle.bottom) {
                                    rectangle = rectangle.copy(bottom = interpolatedPoint.y)
                                }
                            }

                            previousControlPoint = control
                            currentPoint += parameter
                        } else {
                            for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                                val interpolatedPoint = parameter.interpolateSmoothQuadraticBezierCurve(currentPoint, control, t)
                                if (interpolatedPoint.x < rectangle.left) {
                                    rectangle = rectangle.copy(left = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.x > rectangle.right) {
                                    rectangle = rectangle.copy(right = interpolatedPoint.x)
                                }

                                if (interpolatedPoint.y > rectangle.top) {
                                    rectangle = rectangle.copy(top = interpolatedPoint.y)
                                }

                                if (interpolatedPoint.y < rectangle.bottom) {
                                    rectangle = rectangle.copy(bottom = interpolatedPoint.y)
                                }
                            }

                            previousControlPoint = control
                            currentPoint = parameter
                        }
                    }
                }

                is EllipticalArcCurve -> {
                    for (arcParameter in command.parameters) {
                        if (command.variant == CommandVariant.RELATIVE) {
                            val box = arcParameter.computeBoundingBox(command.variant, currentPoint)

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

                            currentPoint += arcParameter.end
                        } else {
                            val box = arcParameter.computeBoundingBox(command.variant, currentPoint)

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

            if (currentPoint.x < rectangle.left) {
                rectangle = rectangle.copy(left = currentPoint.x)
            }

            if (currentPoint.x > rectangle.right) {
                rectangle = rectangle.copy(right = currentPoint.x)
            }

            if (currentPoint.y > rectangle.top) {
                rectangle = rectangle.copy(top = currentPoint.y)
            }

            if (currentPoint.y < rectangle.bottom) {
                rectangle = rectangle.copy(bottom = currentPoint.y)
            }
        }
    }
}
