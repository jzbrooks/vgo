package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo

fun samplePerimeter(commands: List<Command>): List<Point> {
    // todo: avoid an allocation for command variant - maybe move some logic into
    //  top-level functions?
    val absolute = CommandAbsolutizer()
    return absolute.convertToAbsoluteCoordinates(commands).mapNotNull { (it as? ParameterizedCommand<*>)?.parameters?.lastOrNull() as? Point }
}

/**
 * There's _a lot_ of overlap with [com.jzbrooks.vgo.core.optimization.CommandVariant].
 * It is better than using [convertToAbsoluteCoordinates] because that would be O(n)^2
 * TODO: de-dup logic
 */
private class CommandAbsolutizer() {
    private val pathStart = ArrayDeque<Point>()

    // Updated once per process call when computing
    // the other variant of the command. This works
    // because the coordinates are accurate regardless
    // of their absolute or relative nature.
    private lateinit var currentPoint: Point

    fun convertToAbsoluteCoordinates(commands: List<Command>): List<Command> {
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

        val modifiedCommands = mutableListOf<Command>()
        for (i in commands.indices.drop(1)) {
            val command = commands[i]
            val previousCommand = commands.getOrNull(i - 1)

            if (previousCommand is ClosePath && command !is MoveTo) {
                pathStart.addFirst(currentPoint.copy())
            }

            val modifiedCommand =
                when (command) {
                    is MoveTo -> process(command)
                    is LineTo -> process(command)
                    is HorizontalLineTo -> process(command)
                    is VerticalLineTo -> process(command)
                    is CubicBezierCurve -> process(command)
                    is SmoothCubicBezierCurve -> process(command)
                    is QuadraticBezierCurve -> process(command)
                    is SmoothQuadraticBezierCurve -> process(command)
                    is EllipticalArcCurve -> process(command)
                    is ClosePath -> process(command)
                }

            modifiedCommands.add(modifiedCommand)
        }

        return firstMoveTo + modifiedCommands
    }

    private fun process(command: MoveTo): MoveTo {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map { commandPoint ->
                            (commandPoint + currentPoint)
                        }.also { currentPoint = it.last().copy() },
                )
            } else {
                command
            }

        pathStart.addFirst(currentPoint.copy())

        return command
    }

    private fun process(command: LineTo): LineTo {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map { commandPoint ->
                            (commandPoint + currentPoint)
                        }.also { currentPoint = it.last().copy() },
                )
            } else {
                command
            }

        return command
    }

    private fun process(command: HorizontalLineTo): HorizontalLineTo {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map { x ->
                            (x + currentPoint.x)
                        }.also { currentPoint = currentPoint.copy(x = it.last()) },
                )
            } else {
                command
            }

        return command
    }

    private fun process(command: VerticalLineTo): VerticalLineTo {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map { y ->
                            (y + currentPoint.y)
                        }.also { currentPoint = currentPoint.copy(y = it.last()) },
                )
            } else {
                command
            }

        return command
    }

    private fun process(command: CubicBezierCurve): CubicBezierCurve {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map {
                            it.copy(
                                startControl = it.startControl + currentPoint,
                                endControl = it.endControl + currentPoint,
                                end = it.end + currentPoint,
                            )
                        }.also {
                            currentPoint = it.last().end.copy()
                        },
                )
            } else {
                command
            }

        return command
    }

    private fun process(command: SmoothCubicBezierCurve): SmoothCubicBezierCurve {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map {
                            it.copy(
                                endControl = it.endControl + currentPoint,
                                end = it.end + currentPoint,
                            )
                        }.also {
                            currentPoint = it.last().end.copy()
                        },
                )
            } else {
                command
            }

        return command
    }

    private fun process(command: QuadraticBezierCurve): QuadraticBezierCurve {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map {
                            it.copy(
                                control = it.control + currentPoint,
                                end = it.end + currentPoint,
                            )
                        }.also {
                            currentPoint = it.last().end.copy()
                        },
                )
            } else {
                command
            }

        return command
    }

    private fun process(command: SmoothQuadraticBezierCurve): SmoothQuadraticBezierCurve {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map { commandPoint ->
                            commandPoint + currentPoint
                        }.also { currentPoint = it.last().copy() },
                )
            } else {
                command
            }

        return command
    }

    private fun process(command: EllipticalArcCurve): EllipticalArcCurve {
        val command =
            if (command.variant == CommandVariant.RELATIVE) {
                command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters =
                    command.parameters
                        .map {
                            it.copy(end = it.end + currentPoint)
                        }.also {
                            currentPoint = it.last().end.copy()
                        },
                )
            } else {
                command
            }

        return command
    }

    private fun process(command: ClosePath): ClosePath {
        // If there is a close path, there should be a corresponding path start entry on the stack
        currentPoint = pathStart.removeFirst()
        return command
    }
}