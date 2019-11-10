package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.ContainerElement
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.PathElement
import com.jzbrooks.guacamole.core.graphic.command.*
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.util.math.Point
import java.util.*

// todo: handle precision better. Converted commands can be longer because they have more decimal places.
class CommandVariant : Optimization {
    private val subPathStart = Stack<Point>()

    // Updated once per process call when computing
    // the other variant of the command. This works
    // because the coordinates are accurate regardless
    // of their absolute or relative nature.
    private lateinit var currentPoint: Point

    override fun optimize(graphic: Graphic) {
        topDownVisit(graphic)
    }

    private fun topDownVisit(element: Element): Element {
        return when (element) {
            is PathElement -> visit(element)
            is ContainerElement -> element.apply { elements = elements.map(::topDownVisit) }
            else -> element
        }
    }

    private fun visit(element: PathElement): PathElement {
        subPathStart.clear()

        val initialMoveTo = element.commands.first() as MoveTo
        currentPoint = initialMoveTo.parameters.last().copy()

        element.commands = listOf(initialMoveTo) + element.commands.asSequence().drop(1).map { command ->
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

        return element
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

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
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

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
    }

    private fun process(command: HorizontalLineTo): HorizontalLineTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map { x ->
                        (x + currentPoint.x)
                    }.also { currentPoint.x = it.last() }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map { x ->
                        (x - currentPoint.x)
                    }.also { currentPoint.x += it.last() }
            )
        }

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
    }

    private fun process(command: VerticalLineTo): VerticalLineTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(
                    variant = CommandVariant.ABSOLUTE,
                    parameters = command.parameters.map { y ->
                        (y + currentPoint.y)
                    }.also {
                        currentPoint.y = it.last()
                    }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map { y ->
                        (y - currentPoint.y)
                    }.also {
                        currentPoint.y += it.last()
                    }
            )
        }

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
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

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
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

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
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

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
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

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
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

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
    }

    private fun process(command: ClosePath): ClosePath {
        if (subPathStart.isNotEmpty()) {
            currentPoint = subPathStart.pop()
        }

        return command
    }
}