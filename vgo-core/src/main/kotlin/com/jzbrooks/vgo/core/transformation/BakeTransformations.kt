package com.jzbrooks.vgo.core.transformation

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
class BakeTransformations :
    ElementVisitor,
    BottomUpTransformer {
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
                            var intermediatePoint = currentPoint
                            parameters =
                                parameters.map { parameter ->
                                    val point =
                                        if (variant == CommandVariant.RELATIVE) {
                                            val absPoint = intermediatePoint + parameter
                                            intermediatePoint = absPoint
                                            absPoint
                                        } else {
                                            intermediatePoint = parameter
                                            parameter
                                        }

                                    (transform * Vector3(point)).toPoint()
                                }
                            variant = CommandVariant.ABSOLUTE

                            currentPoint = intermediatePoint
                            subPathStart.push(currentPoint)
                        }
                    }

                    is LineTo -> {
                        command.apply {
                            var intermediatePoint = currentPoint
                            parameters =
                                parameters.map { parameter ->
                                    val point =
                                        if (variant == CommandVariant.RELATIVE) {
                                            val absPoint = intermediatePoint + parameter
                                            intermediatePoint = absPoint
                                            absPoint
                                        } else {
                                            intermediatePoint = parameter
                                            parameter
                                        }

                                    (transform * Vector3(point)).toPoint()
                                }
                            variant = CommandVariant.ABSOLUTE

                            currentPoint = intermediatePoint
                        }
                    }

                    is HorizontalLineTo -> {
                        command.run {
                            var intermediateX = currentPoint.x
                            val newParameters =
                                parameters.map { parameter ->
                                    val x =
                                        if (variant == CommandVariant.RELATIVE) {
                                            intermediateX += parameter
                                            intermediateX
                                        } else {
                                            intermediateX = parameter
                                            parameter
                                        }
                                    val point = Point(x, currentPoint.y)

                                    (transform * Vector3(point)).toPoint()
                                }

                            currentPoint = currentPoint.copy(x = intermediateX)
                            LineTo(CommandVariant.ABSOLUTE, newParameters)
                        }
                    }

                    is VerticalLineTo -> {
                        command.run {
                            var intermediateY = currentPoint.y
                            val newParameters =
                                parameters.map { parameter ->
                                    val y =
                                        if (variant == CommandVariant.RELATIVE) {
                                            intermediateY += parameter
                                            intermediateY
                                        } else {
                                            intermediateY = parameter
                                            parameter
                                        }
                                    val point = Point(currentPoint.x, y)

                                    (transform * Vector3(point)).toPoint()
                                }

                            currentPoint = currentPoint.copy(y = intermediateY)
                            LineTo(CommandVariant.ABSOLUTE, newParameters)
                        }
                    }

                    is QuadraticBezierCurve -> {
                        command.apply {
                            var intermediatePoint = currentPoint
                            for (parameter in parameters) {
                                val (control, end) =
                                    if (variant == CommandVariant.RELATIVE) {
                                        val absControl = intermediatePoint + parameter.control
                                        val absEnd = intermediatePoint + parameter.end
                                        intermediatePoint = absEnd
                                        absControl to absEnd
                                    } else {
                                        intermediatePoint = parameter.end
                                        parameter.control to parameter.end
                                    }

                                parameter.control = (transform * Vector3(control)).toPoint()
                                parameter.end = (transform * Vector3(end)).toPoint()
                            }
                            variant = CommandVariant.ABSOLUTE

                            currentPoint = intermediatePoint
                        }
                    }

                    is SmoothQuadraticBezierCurve -> {
                        command.apply {
                            var intermediatePoint = currentPoint
                            parameters =
                                parameters.map { parameter ->
                                    val point =
                                        if (variant == CommandVariant.RELATIVE) {
                                            val absPoint = intermediatePoint + parameter
                                            intermediatePoint = absPoint
                                            absPoint
                                        } else {
                                            intermediatePoint = parameter
                                            parameter
                                        }

                                    (transform * Vector3(point)).toPoint()
                                }
                            variant = CommandVariant.ABSOLUTE

                            currentPoint = intermediatePoint
                        }
                    }

                    is CubicBezierCurve -> {
                        command.apply {
                            var intermediatePoint = currentPoint
                            for (parameter in parameters) {
                                val (startControl, endControl, end) =
                                    if (variant == CommandVariant.RELATIVE) {
                                        val absStartControl = intermediatePoint + parameter.startControl
                                        val absEndControl = intermediatePoint + parameter.endControl
                                        val absEnd = intermediatePoint + parameter.end
                                        intermediatePoint = absEnd
                                        Triple(absStartControl, absEndControl, absEnd)
                                    } else {
                                        intermediatePoint = parameter.end
                                        Triple(parameter.startControl, parameter.endControl, parameter.end)
                                    }

                                parameter.startControl = (transform * Vector3(startControl)).toPoint()
                                parameter.endControl = (transform * Vector3(endControl)).toPoint()
                                parameter.end = (transform * Vector3(end)).toPoint()
                            }
                            variant = CommandVariant.ABSOLUTE

                            currentPoint = intermediatePoint
                        }
                    }

                    is SmoothCubicBezierCurve -> {
                        command.apply {
                            var intermediatePoint = currentPoint
                            for (parameter in parameters) {
                                val (endControl, end) =
                                    if (variant == CommandVariant.RELATIVE) {
                                        val absEndControl = intermediatePoint + parameter.endControl
                                        val absEnd = intermediatePoint + parameter.end
                                        intermediatePoint = absEnd
                                        absEndControl to absEnd
                                    } else {
                                        intermediatePoint = parameter.end
                                        parameter.endControl to parameter.end
                                    }

                                parameter.endControl = (transform * Vector3(endControl)).toPoint()
                                parameter.end = (transform * Vector3(end)).toPoint()
                            }
                            variant = CommandVariant.ABSOLUTE

                            currentPoint = intermediatePoint
                        }
                    }

                    is EllipticalArcCurve -> {
                        command.apply {
                            var intermediatePoint = currentPoint
                            for (parameter in parameters) {
                                val end =
                                    if (variant == CommandVariant.RELATIVE) {
                                        val absEnd = intermediatePoint + parameter.end
                                        intermediatePoint = absEnd
                                        absEnd
                                    } else {
                                        intermediatePoint = parameter.end
                                        parameter.end
                                    }

                                parameter.end = (transform * Vector3(end)).toPoint()
                            }
                            variant = CommandVariant.ABSOLUTE

                            currentPoint = intermediatePoint
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
