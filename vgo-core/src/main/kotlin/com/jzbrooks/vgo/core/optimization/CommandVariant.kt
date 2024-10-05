package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandPrinter
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
import com.jzbrooks.vgo.core.util.math.Point

/**
 * Converts commands to use relative, absolute,
 * or the shortest representation of coordinates
 * @param mode determines the operating mode of the command
 */
class CommandVariant(
    private val mode: Mode,
) : TopDownOptimization {
    private val pathStart = ArrayDeque<Point>()

    // Updated once per process call when computing
    // the other variant of the command. This works
    // because the coordinates are accurate regardless
    // of their absolute or relative nature.
    private lateinit var currentPoint: Point

    override fun visit(graphic: Graphic) {}

    override fun visit(clipPath: ClipPath) {}

    override fun visit(group: Group) {}

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {
        pathStart.clear()
        currentPoint = Point(0f, 0f)

        val firstMoveTo =
            path.commands.take(1).map {
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
        for (i in path.commands.indices.drop(1)) {
            val command = path.commands[i]
            val previousCommand = path.commands.getOrNull(i - 1)

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

        path.commands = firstMoveTo + modifiedCommands
    }

    private fun process(command: MoveTo): MoveTo {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map { commandPoint ->
                                (commandPoint - currentPoint)
                            }.also { currentPoint += it.last() },
                )
            }

        pathStart.addFirst(currentPoint.copy())

        return choose(convertedCommand, command)
    }

    private fun process(command: LineTo): LineTo {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map { commandPoint ->
                                (commandPoint - currentPoint)
                            }.also { currentPoint += it.last() },
                )
            }

        return choose(convertedCommand, command)
    }

    private fun process(command: HorizontalLineTo): HorizontalLineTo {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map { x ->
                                (x - currentPoint.x)
                            }.also { currentPoint = currentPoint.copy(x = currentPoint.x + it.last()) },
                )
            }

        return choose(convertedCommand, command)
    }

    private fun process(command: VerticalLineTo): VerticalLineTo {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map { y ->
                                (y - currentPoint.y)
                            }.also { currentPoint = currentPoint.copy(y = currentPoint.y + it.last()) },
                )
            }

        return choose(convertedCommand, command)
    }

    private fun process(command: CubicBezierCurve): CubicBezierCurve {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map {
                                it.copy(
                                    startControl = it.startControl - currentPoint,
                                    endControl = it.endControl - currentPoint,
                                    end = it.end - currentPoint,
                                )
                            }.also {
                                currentPoint += it.last().end
                            },
                )
            }

        return choose(convertedCommand, command)
    }

    private fun process(command: SmoothCubicBezierCurve): SmoothCubicBezierCurve {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map {
                                it.copy(
                                    endControl = it.endControl - currentPoint,
                                    end = it.end - currentPoint,
                                )
                            }.also {
                                currentPoint += it.last().end
                            },
                )
            }

        return choose(convertedCommand, command)
    }

    private fun process(command: QuadraticBezierCurve): QuadraticBezierCurve {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map {
                                it.copy(
                                    control = it.control - currentPoint,
                                    end = it.end - currentPoint,
                                )
                            }.also {
                                currentPoint += it.last().end
                            },
                )
            }

        return choose(convertedCommand, command)
    }

    private fun process(command: SmoothQuadraticBezierCurve): SmoothQuadraticBezierCurve {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map { commandPoint ->
                                commandPoint - currentPoint
                            }.also {
                                currentPoint += it.last()
                            },
                )
            }

        return choose(convertedCommand, command)
    }

    private fun process(command: EllipticalArcCurve): EllipticalArcCurve {
        val convertedCommand =
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
                command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters =
                        command.parameters
                            .map {
                                it.copy(end = it.end - currentPoint)
                            }.also {
                                currentPoint += it.last().end
                            },
                )
            }

        return choose(convertedCommand, command)
    }

    private fun process(command: ClosePath): ClosePath {
        // If there is a close path, there should be a corresponding path start entry on the stack
        currentPoint = pathStart.removeFirst()
        return command
    }

    private fun <T : ParameterizedCommand<*>> choose(
        convertedCommand: T,
        command: T,
    ): T {
        return when (mode) {
            is Mode.Absolute -> {
                if (convertedCommand.variant == CommandVariant.ABSOLUTE) {
                    convertedCommand
                } else {
                    command
                }
            }
            is Mode.Relative -> {
                if (convertedCommand.variant == CommandVariant.RELATIVE) {
                    convertedCommand
                } else {
                    command
                }
            }
            is Mode.Compact -> {
                return if (mode.printer.print(convertedCommand).length < mode.printer.print(command).length) {
                    convertedCommand
                } else {
                    command
                }
            }
        }
    }

    sealed class Mode {
        data object Absolute : Mode()

        data object Relative : Mode()

        data class Compact(
            val printer: CommandPrinter,
        ) : Mode()
    }
}
