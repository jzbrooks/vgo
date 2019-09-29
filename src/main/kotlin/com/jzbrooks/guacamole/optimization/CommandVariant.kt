package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.ContainerElement
import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic
import com.jzbrooks.guacamole.graphic.PathElement
import com.jzbrooks.guacamole.graphic.command.*
import com.jzbrooks.guacamole.graphic.command.CommandVariant
import java.util.*

class CommandVariant : Optimization {
    // Updated once per process call when computing
    // the other variant of the command. This works
    // because the coordinates are accurate regardless
    // of their absolute or relative nature.
    private var currentPoint = Point(0f, 0f)
    private val subPathStart = Stack<Point>()

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
        element.commands = element.commands.map { command ->
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
        subPathStart.push(currentPoint)

        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(variant = CommandVariant.ABSOLUTE, parameters = command.parameters.map { commandPoint ->
                (commandPoint + currentPoint).also { currentPoint += it }
            })
        } else {
            command.copy(variant = CommandVariant.RELATIVE, parameters = command.parameters.map { commandPoint ->
                (commandPoint - currentPoint).also { currentPoint += it }
            })
        }

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
    }

    private fun process(command: LineTo): LineTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(variant = CommandVariant.ABSOLUTE, parameters = command.parameters.map { commandPoint ->
                (commandPoint + currentPoint).also { currentPoint += it }
            })
        } else {
            command.copy(variant = CommandVariant.RELATIVE, parameters = command.parameters.map { commandPoint ->
                (commandPoint - currentPoint).also { currentPoint += it }
            })
        }

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
    }

    private fun process(command: HorizontalLineTo): HorizontalLineTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(variant = CommandVariant.ABSOLUTE, parameters = command.parameters.map { x ->
                (x + currentPoint.x).also { currentPoint.x += it }
            })
        } else {
            command.copy(variant = CommandVariant.RELATIVE, parameters = command.parameters.map { x ->
                (x - currentPoint.x).also { currentPoint.x += it }
            })
        }

        return if (convertedCommand.toString().length < command.toString().length) {
            convertedCommand
        } else {
            command
        }
    }

    private fun process(command: VerticalLineTo): VerticalLineTo {
        val convertedCommand = if (command.variant == CommandVariant.RELATIVE) {
            command.copy(variant = CommandVariant.ABSOLUTE, parameters = command.parameters.map { y ->
                (y + currentPoint.y).also { currentPoint.y += it }
            })
        } else {
            command.copy(variant = CommandVariant.RELATIVE, parameters = command.parameters.map { y ->
                (y - currentPoint.y).also { currentPoint.y += it }
            })
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
                    ).also { absoluteParam ->
                        currentPoint += absoluteParam.end
                    }
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
                    ).also { relativeParam ->
                        currentPoint += relativeParam.end
                    }
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
                        ).also { absoluteParam ->
                            currentPoint += absoluteParam.end
                        }
                    }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map {
                        it.copy(
                                endControl = it.endControl - currentPoint,
                                end = it.end - currentPoint
                        ).also { relativeParam ->
                            currentPoint += relativeParam.end
                        }
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
                        ).also { absoluteParam ->
                            currentPoint += absoluteParam.end
                        }
                    }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map {
                        it.copy(
                                control = it.control - currentPoint,
                                end = it.end - currentPoint
                        ).also { relativeParam ->
                            currentPoint += relativeParam.end
                        }
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
            command.copy(variant = CommandVariant.ABSOLUTE, parameters = command.parameters.map { commandPoint ->
                (commandPoint + currentPoint).also { currentPoint += it }
            })
        } else {
            command.copy(variant = CommandVariant.RELATIVE, parameters = command.parameters.map { commandPoint ->
                (commandPoint - currentPoint).also { currentPoint += it }
            })
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
                        it.copy(end = it.end + currentPoint).also { absoluteParam ->
                            currentPoint += absoluteParam.end
                        }
                    }
            )
        } else {
            command.copy(
                    variant = CommandVariant.RELATIVE,
                    parameters = command.parameters.map {
                        it.copy(end = it.end - currentPoint).also { relativeParam ->
                            currentPoint += relativeParam.end
                        }
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
        currentPoint = subPathStart.pop()
        return command
    }
}