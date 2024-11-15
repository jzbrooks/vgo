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
                        val command1 =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<Point, Point> { commandPoint ->
                                                (commandPoint + currentPoint)
                                            }.also<List<Point>> { currentPoint = it.last().copy() },
                                )
                            } else {
                                command
                            }
                        pathStart.addFirst(currentPoint.copy())
                        command1
                    }

                    is LineTo -> {
                        val command1 =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<Point, Point> { commandPoint ->
                                                (commandPoint + currentPoint)
                                            }.also<List<Point>> { currentPoint = it.last().copy() },
                                )
                            } else {
                                command
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

                        command1
                    }

                    is HorizontalLineTo -> {
                        val command1 =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<Float, Float> { x ->
                                                (x + currentPoint.x)
                                            }.also<List<Float>> { currentPoint = currentPoint.copy(x = it.last()) },
                                )
                            } else {
                                command
                            }

                        if (currentPoint.x < rectangle.left) {
                            rectangle = rectangle.copy(left = currentPoint.x)
                        }

                        if (currentPoint.x > rectangle.right) {
                            rectangle = rectangle.copy(right = currentPoint.x)
                        }

                        command1
                    }

                    is VerticalLineTo -> {
                        val command1 =
                            if (command.variant == CommandVariant.RELATIVE) {
                                command.copy(
                                    variant = CommandVariant.ABSOLUTE,
                                    parameters =
                                        command.parameters
                                            .map<Float, Float> { y ->
                                                (y + currentPoint.y)
                                            }.also<List<Float>> { currentPoint = currentPoint.copy(y = it.last()) },
                                )
                            } else {
                                command
                            }

                        if (currentPoint.y > rectangle.top) {
                            rectangle = rectangle.copy(top = currentPoint.y)
                        }

                        if (currentPoint.y < rectangle.bottom) {
                            rectangle = rectangle.copy(bottom = currentPoint.y)
                        }

                        command1
                    }

                    is CubicBezierCurve -> {
                        val command1 =
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
                                            }.also<List<CubicBezierCurve.Parameter>> {
                                                currentPoint = it.last().end.copy()
                                            },
                                )
                            } else {
                                command
                            }

                        previousControlPoint = command1.parameters.last().endControl

                        for (t in listOf(0f, 0.25f, 0.75f, 1f)) {
                            val interpolatedPoint = command1.interpolateRelative(t)
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

                        command1
                    }

                    is SmoothCubicBezierCurve -> {
                        val command1 =
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

                        previousControlPoint = command1.parameters.last().endControl

                        for (t in listOf(0f, 0.25f, 0.75f, 1f)) {
                            val interpolatedPoint = command1.interpolateRelative(previousControlPoint, t)
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

                        command1
                    }

                    is QuadraticBezierCurve -> {
                        val command1 =
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

                        previousControlPoint = command1.parameters.last().control

                        for (t in listOf(0f, 0.25f, 0.75f, 1f)) {
                            val interpolatedPoint = command1.interpolateRelative(t)
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

                        command1
                    }

                    is SmoothQuadraticBezierCurve ->
                        if (command.variant == CommandVariant.RELATIVE) {
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
                        }.also { curve ->
                            previousControlPoint = curve.parameters.last()

                            for (t in listOf(0f, 0.25f, 0.75f, 1f)) {
                                val interpolatedPoint = curve.interpolateRelative(previousControlPoint, t)
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
                        }

                    is EllipticalArcCurve -> {
                        val command1 =
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

                        val box = command1.computeBoundingBox(Point.ZERO)

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

                        command1
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
