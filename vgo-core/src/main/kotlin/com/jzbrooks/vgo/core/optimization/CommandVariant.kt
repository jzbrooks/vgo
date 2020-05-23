package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.util.math.Point
import java.util.*

class CommandVariant(private val mode: Mode) : TopDownOptimization, PathElementVisitor {
    private val pathStart = Stack<Point>()

    // Updated once per process call when computing
    // the other variant of the command. This works
    // because the coordinates are accurate regardless
    // of their absolute or relative nature.
    private lateinit var currentPoint: Point

    override fun visit(pathElement: PathElement) {
        pathStart.clear()
        currentPoint = Point(0f, 0f)

        val firstMoveTo = pathElement.commands.take(1).map {
            val moveTo = it as MoveTo
            // The first M/m is always treated as absolute.
            // This is accomplished by initializing the current point
            // to the origin and adding the next current point values.
            // Absolute values are relative to the origin, so += means
            // the same thing here.
            currentPoint += moveTo.parameters.last()
            pathStart.push(currentPoint.copy())
            moveTo
        }

        val commands = pathElement.commands.drop(1).map { command ->
            when (command) {
                is MoveTo -> process(command)
                is LineTo -> process(command)
                is HorizontalLineTo -> process(command)
                is VerticalLineTo -> process(command)
                is CubicBezierCurve -> process(command)
                is ShortcutCubicBezierCurve -> process(command)
                is QuadraticBezierCurve -> process(command)
                is ShortcutQuadraticBezierCurve -> process(command)
                is EllipticalArcCurve -> process(command)
                is ClosePath -> process(command)
                else -> throw IllegalStateException("Unsupported command encountered: $command")
            }
        }

        pathElement.commands = firstMoveTo + commands
    }

    private fun process(command: MoveTo): MoveTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map { commandPoint ->
                        (commandPoint + currentPoint)
                    }.also { currentPoint = it.last().copy() }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map { commandPoint ->
                        (commandPoint - currentPoint)
                    }.also { currentPoint += it.last() }
            )
        }

        pathStart.push(currentPoint.copy())

        return choose(convertedCommand, command)
    }

    private fun process(command: LineTo): LineTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map { commandPoint ->
                        (commandPoint + currentPoint)
                    }.also { currentPoint = it.last().copy() }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map { commandPoint ->
                        (commandPoint - currentPoint)
                    }.also { currentPoint += it.last() }
            )
        }

        return choose(convertedCommand, command)
    }

    private fun process(command: HorizontalLineTo): HorizontalLineTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map { x ->
                        (x + currentPoint.x)
                    }.also { currentPoint = currentPoint.copy(x = it.last()) }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map { x ->
                        (x - currentPoint.x)
                    }.also { currentPoint = currentPoint.copy(x = currentPoint.x + it.last()) }
            )
        }

        return choose(convertedCommand, command)
    }

    private fun process(command: VerticalLineTo): VerticalLineTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map { y ->
                        (y + currentPoint.y)
                    }.also { currentPoint = currentPoint.copy(y = it.last()) }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map { y ->
                        (y - currentPoint.y)
                    }.also { currentPoint = currentPoint.copy(y = currentPoint.y + it.last()) }
            )
        }

        return choose(convertedCommand, command)
    }

    private fun process(command: CubicBezierCurve): CubicBezierCurve {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                variant = CommandVariant.ABSOLUTE,
                parameters = command.parameters.map {
                    it.copy(
                            startControl = it.startControl + currentPoint,
                            endControl = it.endControl + currentPoint,
                            end = it.end + currentPoint
                    )
                }.also {
                    currentPoint = it.last().end.copy()
                }
            )
        } else {
            command.copy(
                variant = CommandVariant.RELATIVE,
                parameters = command.parameters.map {
                    it.copy(
                            startControl = it.startControl - currentPoint,
                            endControl = it.endControl - currentPoint,
                            end = it.end - currentPoint
                    )
                }.also {
                    currentPoint += it.last().end
                }
            )
        }

        return choose(convertedCommand, command)
    }

    private fun process(command: ShortcutCubicBezierCurve): ShortcutCubicBezierCurve {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map {
                        it.copy(
                                endControl = it.endControl + currentPoint,
                                end = it.end + currentPoint
                        )
                    }.also {
                        currentPoint = it.last().end.copy()
                    }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map {
                        it.copy(
                                endControl = it.endControl - currentPoint,
                                end = it.end - currentPoint
                        )
                    }.also {
                        currentPoint += it.last().end
                    }
            )
        }

        return choose(convertedCommand, command)
    }

    private fun process(command: QuadraticBezierCurve): QuadraticBezierCurve {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map {
                        it.copy(
                                control = it.control + currentPoint,
                                end = it.end + currentPoint
                        )
                    }.also {
                        currentPoint = it.last().end.copy()
                    }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map {
                        it.copy(
                                control = it.control - currentPoint,
                                end = it.end - currentPoint
                        )
                    }.also {
                        currentPoint += it.last().end
                    }
            )
        }

        return choose(convertedCommand, command)
    }

    private fun process(command: ShortcutQuadraticBezierCurve): ShortcutQuadraticBezierCurve {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map { commandPoint ->
                        (commandPoint + currentPoint)
                    }.also { currentPoint = it.last().copy() }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map { commandPoint ->
                        (commandPoint - currentPoint)
                    }.also {
                        currentPoint += it.last()
                    }
            )
        }

        return choose(convertedCommand, command)
    }

    private fun process(command: EllipticalArcCurve): EllipticalArcCurve {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map {
                        it.copy(end = it.end + currentPoint)
                    }.also {
                        currentPoint = it.last().end.copy()
                    }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map {
                        it.copy(end = it.end - currentPoint)
                    }.also {
                        currentPoint += it.last().end
                    }
            )
        }

        return choose(convertedCommand, command)
    }

    private fun process(command: ClosePath): ClosePath {
        // If there is a close path, there should be a corresponding path start entry on the stack
        currentPoint = pathStart.pop()
        return command
    }

    private fun <T : ParameterizedCommand<*>> choose(convertedCommand: T, command: T): T {
        return when (mode) {
            is Mode.Absolute -> {
                if (convertedCommand.variant == CommandVariant.ABSOLUTE)
                    convertedCommand
                else
                    command
            }
            is Mode.Relative -> {
                if (convertedCommand.variant == CommandVariant.RELATIVE)
                    convertedCommand
                else
                    command
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
        object Absolute : Mode()
        object Relative : Mode()
        data class Compact(val printer: CommandPrinter) : Mode()
    }
}