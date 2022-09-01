package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.*
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.util.math.Point
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Mat3
import java.util.*

/**
 * Apply transformations to paths command coordinates in a group
 */
class BakeTransformations : ElementVisitor, BottomUpOptimization {

    override fun visit(graphic: Graphic) {}
    override fun visit(clipPath: ClipPath) {}
    override fun visit(extra: Extra) {}
    override fun visit(path: Path) {}

    override fun visit(group: Group) {
        group.elements = group.elements.flatMap {
            if (it is Group && areElementsRelocatable(it)) it.elements
            else listOf(it)
        }

        val groupTransform = group.transform

        if (group.elements.any { it !is Path } || groupTransform == Mat3.identity())
            return

        for (child in group.elements) {
            applyTransform(child as Path, groupTransform)
        }

        // Transform is baked. We don't want to apply it twice.
        group.transform = Mat3.identity()
    }

    private fun areElementsRelocatable(group: Group): Boolean {
        return group.id == null &&
            group.transform == Mat3.identity() &&
            group.foreign.isEmpty() &&
            group.elements.all { it is Path }
    }

    private fun applyTransform(path: Path, transform: Mat3) {
        if (path.commands.isEmpty()) return

        val subPathStart = Stack<Float2>()

        val initialMoveTo = path.commands.first() as MoveTo
        var currentPoint = initialMoveTo.parameters.last().copy()
        val transformedMoveTo = initialMoveTo.apply {
            parameters = parameters.map { val t = (transform * Float3(it.x, it.y, 1f)); Float2(t.x, t.y) }
        }

        path.commands = listOf(transformedMoveTo) + path.commands.asSequence().drop(1).map { command ->
            when (command) {
                is MoveTo -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.reduce(Float2::plus)
                        } else {
                            parameters.last()
                        }

                        parameters = parameters.map { parameter ->
                            val point = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter
                            } else {
                                parameter
                            }

                            val transformedPoint = (transform * Float3(point.x, point.y, 1f))
                            Float2(transformedPoint.x, transformedPoint.y)
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                        subPathStart.push(currentPoint)
                    }
                }
                is LineTo -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.reduce(Float2::plus)
                        } else {
                            parameters.last()
                        }

                        parameters = parameters.map { parameter ->
                            val point = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter
                            } else {
                                parameter
                            }

                            val transformedPoint = (transform * Float3(point.x, point.y, 1f))
                            Float2(transformedPoint.x, transformedPoint.y)
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is HorizontalLineTo -> {
                    command.run {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint.x + parameters.reduce(Float::plus)
                        } else {
                            parameters.last()
                        }

                        val newParameters = parameters.map { parameter ->
                            val x = if (variant == CommandVariant.RELATIVE) {
                                currentPoint.x + parameter
                            } else {
                                parameter
                            }
                            val point = Point(x, currentPoint.y)

                            val transformedPoint = (transform * Float3(point.x, point.y, 1f))
                            Float2(transformedPoint.x, transformedPoint.y)
                        }

                        currentPoint = currentPoint.copy(x = newCurrentPoint)
                        LineTo(CommandVariant.ABSOLUTE, newParameters)
                    }
                }
                is VerticalLineTo -> {
                    command.run {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint.y + parameters.reduce(Float::plus)
                        } else {
                            parameters.last()
                        }

                        val newParameters = parameters.map { parameter ->
                            val y = if (variant == CommandVariant.RELATIVE) {
                                currentPoint.y + parameter
                            } else {
                                parameter
                            }
                            val point = Point(currentPoint.x, y)

                            val transformedPoint = (transform * Float3(point.x, point.y, 1f))
                            Float2(transformedPoint.x, transformedPoint.y)
                        }

                        currentPoint = currentPoint.copy(y = newCurrentPoint)
                        LineTo(CommandVariant.ABSOLUTE, newParameters)
                    }
                }
                is QuadraticBezierCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.map(QuadraticBezierCurve.Parameter::end).reduce(Float2::plus)
                        } else {
                            parameters.last().end
                        }

                        for (parameter in parameters) {
                            val control = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.control
                            } else {
                                parameter.control
                            }

                            val end = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.end
                            } else {
                                parameter.end
                            }

                            val transformedControl = (transform * Float3(control.x, control.y, 1f))
                            val transformedEnd = (transform * Float3(end.x, end.y, 1f))

                            parameter.control = Float2(transformedControl.x, transformedControl.y)
                            parameter.end = Float2(transformedEnd.x, transformedEnd.y)
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is SmoothQuadraticBezierCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.reduce(Float2::plus)
                        } else {
                            parameters.last()
                        }

                        parameters = parameters.map { parameter ->
                            val point = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter
                            } else {
                                parameter
                            }

                            val transformedPoint = (transform * Float3(point.x, point.y, 1f))
                            Float2(transformedPoint.x, transformedPoint.y)
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is CubicBezierCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.map(CubicBezierCurve.Parameter::end).reduce(Float2::plus)
                        } else {
                            parameters.last().end
                        }

                        for (parameter in parameters) {
                            val startControl = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.startControl
                            } else {
                                parameter.startControl
                            }

                            val endControl = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.endControl
                            } else {
                                parameter.endControl
                            }

                            val end = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.end
                            } else {
                                parameter.end
                            }

                            val transformedStartControl = (transform * Float3(startControl.x, startControl.y, 1f))
                            val transformedEndControl = (transform * Float3(endControl.x, endControl.y, 1f))
                            val transformedEnd = (transform * Float3(end.x, end.y, 1f))

                            parameter.startControl = Float2(transformedStartControl.x, transformedStartControl.y)
                            parameter.endControl = Float2(transformedEndControl.x, transformedEndControl.y)
                            parameter.end = Float2(transformedEnd.x, transformedEnd.y)
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is SmoothCubicBezierCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.map(SmoothCubicBezierCurve.Parameter::end).reduce(Float2::plus)
                        } else {
                            parameters.last().end
                        }

                        for (parameter in parameters) {
                            val endControl = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.endControl
                            } else {
                                parameter.endControl
                            }

                            val end = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.end
                            } else {
                                parameter.end
                            }

                            val transformedEndControl = (transform * Float3(endControl.x, endControl.y, 1f))
                            val transformedEnd = (transform * Float3(end.x, end.y, 1f))

                            parameter.endControl = Float2(transformedEndControl.x, transformedEndControl.y)
                            parameter.end = Float2(transformedEnd.x, transformedEnd.y)
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is EllipticalArcCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.map(EllipticalArcCurve.Parameter::end).reduce(Float2::plus)
                        } else {
                            parameters.last().end
                        }

                        for (parameter in parameters) {
                            val end = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.end
                            } else {
                                parameter.end
                            }

                            val transformedEnd = (transform * Float3(end.x, end.y, 1f))
                            parameter.end = Float2(transformedEnd.x, transformedEnd.y)
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is ClosePath -> {
                    if (subPathStart.isNotEmpty()) {
                        currentPoint = subPathStart.pop()
                    }

                    command
                }
            }
        }
    }
}
