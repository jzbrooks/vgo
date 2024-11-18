package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.*

// TODO:
//   1. Cubic works - yes
//   2. Quad works - yes
//   3. Smooth quad - ?
//   4. Smooth cubic - yes
//   5. Arc - ?
//   6. Ensure point tracking is correct for every command type - yes
//   7. This needs to work with poly-commands (where commands have more than one set of parameters)
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

    fun findBoundingBox(commands: List<Command>): Rectangle {
        pathStart.clear()
        currentPoint = Point(0f, 0f)

        val firstMoveTo =
            commands.take(1).map {
                val moveTo = it as MoveTo
                // The first M/m is always treated as absolute.
                // This is accomplished by initializing the current point
                // to the origin and adding the next current point values.
                // Absolute values are relative to the origin, so += means
                // the same thing here.
                currentPoint += moveTo.parameters.last()
                pathStart.addFirst(currentPoint.copy())
                moveTo
            }

        var rectangle = Rectangle(currentPoint.x, currentPoint.y, currentPoint.x, currentPoint.y)

        val modifiedCommands = mutableListOf<Command>()
        for (i in commands.indices.drop(1)) {
            val command = commands[i]
            val previousCommand = commands.getOrNull(i - 1)

            if (previousCommand is ClosePath && command !is MoveTo) {
                pathStart.addFirst(currentPoint.copy())
            }

            val modifiedCommand =
                when (command) {
                    is MoveTo -> {
                        val lineTo =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<Point, Point> { commandPoint -> commandPoint + currentPoint },
                                )
                            } else {
                                command
                            }
                        currentPoint = command.parameters.last()
                        pathStart.addFirst(currentPoint.copy())
                        lineTo
                    }

                    is LineTo -> {
                        val lineTo =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<Point, Point> { commandPoint -> commandPoint + currentPoint }
                                )
                            } else {
                                command
                            }

                        currentPoint = lineTo.parameters.last()

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

                        lineTo
                    }

                    is HorizontalLineTo -> {
                        val lineTo =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<Float, Float> { x -> x + currentPoint.x }
                                )
                            } else {
                                command
                            }

                        currentPoint = currentPoint.copy(x = lineTo.parameters.last())

                        if (currentPoint.x < rectangle.left) {
                            rectangle = rectangle.copy(left = currentPoint.x)
                        }

                        if (currentPoint.x > rectangle.right) {
                            rectangle = rectangle.copy(right = currentPoint.x)
                        }

                        lineTo
                    }

                    is VerticalLineTo -> {
                        val lineTo =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<Float, Float> { y -> y + currentPoint.y }
                                )
                            } else {
                                command
                            }

                        currentPoint = currentPoint.copy(y = lineTo.parameters.last())

                        if (currentPoint.y > rectangle.top) {
                            rectangle = rectangle.copy(top = currentPoint.y)
                        }

                        if (currentPoint.y < rectangle.bottom) {
                            rectangle = rectangle.copy(bottom = currentPoint.y)
                        }

                        lineTo
                    }

                    is CubicBezierCurve -> {
                        val prev = currentPoint
                        val curve =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<CubicBezierCurve.Parameter, CubicBezierCurve.Parameter> {
                                                it.copy(
                                                    startControl = it.startControl + currentPoint,
                                                    endControl = it.endControl + currentPoint,
                                                    end = it.end + currentPoint,
                                                )
                                            }
                                )
                            } else {
                                command
                            }

                        for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                            val interpolatedPoint = curve.interpolate(prev, t)
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

                        previousControlPoint = curve.parameters.last().endControl
                        currentPoint = curve.parameters.last().end
                        curve
                    }

                    is SmoothCubicBezierCurve -> {
                        val prev = currentPoint
                        val curve =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<SmoothCubicBezierCurve.Parameter, SmoothCubicBezierCurve.Parameter> {
                                                it.copy(
                                                    endControl = it.endControl + currentPoint,
                                                    end = it.end + currentPoint,
                                                )
                                            }.also<List<SmoothCubicBezierCurve.Parameter>> {
                                                currentPoint = it.last().end.copy()
                                            },
                                )
                            } else {
                                command
                            }

                        for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                            val interpolatedPoint = curve.interpolate(prev, previousControlPoint, t)
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

                        previousControlPoint = curve.parameters.last().endControl
                        currentPoint = curve.parameters.last().end
                        curve
                    }

                    is QuadraticBezierCurve -> {
                        val prev = currentPoint
                        val curve =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<QuadraticBezierCurve.Parameter, QuadraticBezierCurve.Parameter> {
                                                it.copy(
                                                    control = it.control + currentPoint,
                                                    end = it.end + currentPoint,
                                                )
                                            }.also<List<QuadraticBezierCurve.Parameter>> {
                                                currentPoint = it.last().end.copy()
                                            },
                                )
                            } else {
                                command
                            }

                        for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                            val interpolatedPoint = curve.interpolate(prev, t)
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

                        previousControlPoint = curve.parameters.last().control
                        currentPoint = curve.parameters.last().end
                        curve
                    }

                    is SmoothQuadraticBezierCurve -> {
                        val prev = currentPoint
                        val curve = if (command.variant == CommandVariant.RELATIVE) {
                            command.copy(
                                variant = CommandVariant.ABSOLUTE,
                                parameters =
                                    command.parameters
                                        .map<Point, Point> { commandPoint ->
                                            commandPoint + currentPoint
                                        }.also<List<Point>> { currentPoint = it.last().copy() },
                            )
                        } else {
                            command
                        }

                        for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                            val interpolatedPoint = curve.interpolate(prev, previousControlPoint, t)
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

                        currentPoint = curve.parameters.last()
                        curve
                    }

                    is EllipticalArcCurve -> {
                        val prev = currentPoint
                        val curve =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<EllipticalArcCurve.Parameter, EllipticalArcCurve.Parameter> {
                                                it.copy(end = it.end + currentPoint)
                                            }.also<List<EllipticalArcCurve.Parameter>> {
                                                currentPoint = it.last().end.copy()
                                            },
                                )
                            } else {
                                command
                            }

                        val box = curve.computeBoundingBox(prev)

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
                            rectangle = rectangle.copy(top = box.bottom)
                        }

                        currentPoint = curve.parameters.last().end
                        curve
                    }

                    is ClosePath -> {
                        // If there is a close path, there should be a corresponding path start entry on the stack
                        currentPoint = pathStart.removeFirst()
                        command
                    }
                }

            modifiedCommands.add(modifiedCommand)
        }

        return rectangle
    }
}
