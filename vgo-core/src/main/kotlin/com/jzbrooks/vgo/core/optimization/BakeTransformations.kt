package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ElementVisitor
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
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

/**
 * Apply transformations to paths command coordinates in a group
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Has been relocated to the transformation package",
    replaceWith = ReplaceWith("com.jzbrooks.vgo.core.transformation.BakeTransformation"),
)
class BakeTransformations :
    ElementVisitor,
    BottomUpOptimization {
    override fun visit(graphic: Graphic) {}

    override fun visit(clipPath: ClipPath) {}

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {}

    override fun visit(group: Group) {
        group.elements =
            group.elements.flatMap {
                if (it is Group && areElementsRelocatable(it)) {
                    it.elements
                } else {
                    listOf(it)
                }
            }

        val groupTransform = group.transform

        if (group.elements.any { it !is Path } || groupTransform.contentsEqual(Matrix3.IDENTITY)) {
            return
        }

        for (child in group.elements) {
            applyTransform(child as Path, groupTransform)
        }

        // Transform is baked. We don't want to apply it twice.
        group.transform = Matrix3.IDENTITY
    }

    private fun areElementsRelocatable(group: Group): Boolean =
        group.id == null &&
            group.transform.contentsEqual(Matrix3.IDENTITY) &&
            group.foreign.isEmpty() &&
            group.elements.all { it is Path }

    private fun applyTransform(
        path: Path,
        transform: Matrix3,
    ) {
        if (path.commands.isEmpty()) return

        val subPathStart = Stack<Point>()

        val initialMoveTo = path.commands.first() as MoveTo
        var currentPoint = initialMoveTo.parameters.last().copy()
        val transformedMoveTo =
            initialMoveTo.apply {
                parameters = parameters.map { (transform * Vector3(it)).toPoint() }
            }

        path.commands = listOf(transformedMoveTo) +
            path.commands.asSequence().drop(1).map { command ->
                when (command) {
                    is MoveTo -> {
                        command.apply {
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint + parameters.reduce(Point::plus)
                                } else {
                                    parameters.last()
                                }

                            parameters =
                                parameters.map { parameter ->
                                    val point =
                                        if (variant == CommandVariant.RELATIVE) {
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
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint + parameters.reduce(Point::plus)
                                } else {
                                    parameters.last()
                                }

                            parameters =
                                parameters.map { parameter ->
                                    val point =
                                        if (variant == CommandVariant.RELATIVE) {
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
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint.x + parameters.reduce(Float::plus)
                                } else {
                                    parameters.last()
                                }

                            val newParameters =
                                parameters.map { parameter ->
                                    val x =
                                        if (variant == CommandVariant.RELATIVE) {
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
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint.y + parameters.reduce(Float::plus)
                                } else {
                                    parameters.last()
                                }

                            val newParameters =
                                parameters.map { parameter ->
                                    val y =
                                        if (variant == CommandVariant.RELATIVE) {
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
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint + parameters.map(QuadraticBezierCurve.Parameter::end).reduce(Point::plus)
                                } else {
                                    parameters.last().end
                                }

                            for (parameter in parameters) {
                                val control =
                                    if (variant == CommandVariant.RELATIVE) {
                                        currentPoint + parameter.control
                                    } else {
                                        parameter.control
                                    }

                                val end =
                                    if (variant == CommandVariant.RELATIVE) {
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
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint + parameters.reduce(Point::plus)
                                } else {
                                    parameters.last()
                                }

                            parameters =
                                parameters.map { parameter ->
                                    val point =
                                        if (variant == CommandVariant.RELATIVE) {
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
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint + parameters.map(CubicBezierCurve.Parameter::end).reduce(Point::plus)
                                } else {
                                    parameters.last().end
                                }

                            for (parameter in parameters) {
                                val startControl =
                                    if (variant == CommandVariant.RELATIVE) {
                                        currentPoint + parameter.startControl
                                    } else {
                                        parameter.startControl
                                    }

                                val endControl =
                                    if (variant == CommandVariant.RELATIVE) {
                                        currentPoint + parameter.endControl
                                    } else {
                                        parameter.endControl
                                    }

                                val end =
                                    if (variant == CommandVariant.RELATIVE) {
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
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint + parameters.map(SmoothCubicBezierCurve.Parameter::end).reduce(Point::plus)
                                } else {
                                    parameters.last().end
                                }

                            for (parameter in parameters) {
                                val endControl =
                                    if (variant == CommandVariant.RELATIVE) {
                                        currentPoint + parameter.endControl
                                    } else {
                                        parameter.endControl
                                    }

                                val end =
                                    if (variant == CommandVariant.RELATIVE) {
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
                            val newCurrentPoint =
                                if (variant == CommandVariant.RELATIVE) {
                                    currentPoint + parameters.map(EllipticalArcCurve.Parameter::end).reduce(Point::plus)
                                } else {
                                    parameters.last().end
                                }

                            for (parameter in parameters) {
                                val end =
                                    if (variant == CommandVariant.RELATIVE) {
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
                }
            }
    }
}
