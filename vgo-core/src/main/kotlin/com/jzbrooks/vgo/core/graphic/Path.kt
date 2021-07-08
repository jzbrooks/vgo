package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Color
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
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.Vector3
import java.util.Stack

data class Path(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    var commands: List<Command>,
    val fill: Color,
    val fillRule: FillRule,
    val stroke: Color,
    val strokeWidth: Float,
    val strokeLineCap: LineCap,
    val strokeLineJoin: LineJoin,
    val strokeMiterLimit: Float,
) : Element {

    override fun accept(visitor: ElementVisitor) = visitor.visit(this)

    fun hasSameAttributes(other: Path): Boolean {
        return id == other.id &&
            foreign == other.foreign &&
            fill == other.fill &&
            fillRule == other.fillRule &&
            stroke == other.stroke &&
            strokeWidth == other.strokeWidth &&
            strokeLineCap == other.strokeLineCap &&
            strokeLineJoin == other.strokeLineJoin &&
            strokeMiterLimit == other.strokeMiterLimit
    }

    fun applyTransform(transform: Matrix3) {
        if (commands.isEmpty()) return

        val subPathStart = Stack<Point>()

        val initialMoveTo = commands.first() as MoveTo
        var currentPoint = initialMoveTo.parameters.last().copy()
        val transformedMoveTo = initialMoveTo.apply {
            parameters = parameters.map { (transform * Vector3(it)).toPoint() }
        }

        commands = listOf(transformedMoveTo) + commands.asSequence().drop(1).map { command ->
            when (command) {
                is MoveTo -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.reduce(Point::plus)
                        } else {
                            parameters.last()
                        }

                        parameters = parameters.map { parameter ->
                            val point = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter
                            } else {
                                parameter
                            }

                            (transform * Vector3(point)).toPoint()
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                        subPathStart.push(currentPoint)
                    }
                }
                is LineTo -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.reduce(Point::plus)
                        } else {
                            parameters.last()
                        }

                        parameters = parameters.map { parameter ->
                            val point = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter
                            } else {
                                parameter
                            }

                            (transform * Vector3(point)).toPoint()
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

                            (transform * Vector3(point)).toPoint()
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

                            (transform * Vector3(point)).toPoint()
                        }

                        currentPoint = currentPoint.copy(y = newCurrentPoint)
                        LineTo(CommandVariant.ABSOLUTE, newParameters)
                    }
                }
                is QuadraticBezierCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.map(QuadraticBezierCurve.Parameter::end).reduce(Point::plus)
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

                            parameter.control = (transform * Vector3(control)).toPoint()
                            parameter.end = (transform * Vector3(end)).toPoint()
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is SmoothQuadraticBezierCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.reduce(Point::plus)
                        } else {
                            parameters.last()
                        }

                        parameters = parameters.map { parameter ->
                            val point = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter
                            } else {
                                parameter
                            }

                            (transform * Vector3(point)).toPoint()
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is CubicBezierCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.map(CubicBezierCurve.Parameter::end).reduce(Point::plus)
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

                            parameter.startControl = (transform * Vector3(startControl)).toPoint()
                            parameter.endControl = (transform * Vector3(endControl)).toPoint()
                            parameter.end = (transform * Vector3(end)).toPoint()
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is SmoothCubicBezierCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.map(SmoothCubicBezierCurve.Parameter::end).reduce(Point::plus)
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

                            parameter.endControl = (transform * Vector3(endControl)).toPoint()
                            parameter.end = (transform * Vector3(end)).toPoint()
                        }
                        variant = CommandVariant.ABSOLUTE

                        currentPoint = newCurrentPoint
                    }
                }
                is EllipticalArcCurve -> {
                    command.apply {
                        val newCurrentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.map(EllipticalArcCurve.Parameter::end).reduce(Point::plus)
                        } else {
                            parameters.last().end
                        }

                        for (parameter in parameters) {
                            val end = if (variant == CommandVariant.RELATIVE) {
                                currentPoint + parameter.end
                            } else {
                                parameter.end
                            }

                            parameter.end = (transform * Vector3(end)).toPoint()
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
                else -> throw IllegalStateException("Unexpected command: $command")
            }
        }
    }

    enum class FillRule {
        NON_ZERO,
        EVEN_ODD,
    }

    enum class LineCap {
        BUTT,
        ROUND,
        SQUARE,
    }

    enum class LineJoin {
        MITER,
        ROUND,
        BEVEL,
        ARCS,
        MITER_CLIP,
    }
}
