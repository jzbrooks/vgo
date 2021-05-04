package com.jzbrooks.vgo.svg.optimization

import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PathElement
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
import com.jzbrooks.vgo.core.optimization.GroupVisitor
import com.jzbrooks.vgo.core.optimization.TopDownOptimization
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.Vector3
import java.util.Stack

/**
 * Apply transformations to paths command coordinates in a group
 * and remove the transformations from the group
 */
class BakeTransformations : TopDownOptimization, GroupVisitor {
    override fun visit(group: Group) {
        bakeIntoGroup(group)
    }

    private fun bakeIntoGroup(group: Group) {
        val groupTransforms = group.attributes.keys intersect transformationPropertyNames

        if (groupTransforms.isNotEmpty()) {
            val groupTransform = computeTransformationMatrix(group)

            // We can only do transform baking if everything in the group can be transform baked
            // todo: handle baking nested groups
            if (group.elements.count { it is PathElement } == group.elements.size) {
                for (child in group.elements) {
                    if (child is PathElement) {
                        applyTransform(child, groupTransform)
                    }
                }

                for (transformAttribute in groupTransforms) {
                    group.attributes.remove(transformAttribute)
                }
            }
        }
    }

    private fun applyTransform(element: PathElement, transform: Matrix3) {
        val subPathStart = Stack<Point>()

        val initialMoveTo = element.commands.first() as MoveTo
        var currentPoint = initialMoveTo.parameters.last().copy()
        val transformedMoveTo = initialMoveTo.apply {
            parameters = parameters.map { (transform * Vector3(it)).toPoint() }
        }

        element.commands = listOf(transformedMoveTo) + element.commands.asSequence().drop(1).map { command ->
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
                            currentPoint + parameters.map { it.end }.reduce(Point::plus)
                        } else {
                            parameters.map { it.end }.last()
                        }

                        parameters.forEach { parameter ->
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
                            currentPoint + parameters.map { it.end }.reduce(Point::plus)
                        } else {
                            parameters.map { it.end }.last()
                        }

                        parameters.forEach { parameter ->
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
                            currentPoint + parameters.map { it.end }.reduce(Point::plus)
                        } else {
                            parameters.map { it.end }.last()
                        }

                        parameters.forEach { parameter ->
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
                            currentPoint + parameters.map { it.end }.reduce(Point::plus)
                        } else {
                            parameters.map { it.end }.last()
                        }

                        parameters.forEach { parameter ->
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

    private fun computeTransformationMatrix(group: Group): Matrix3 {
        // todo: handle other transform types
        val transformValue = group.attributes["transform"]
        return if (transformValue != null) {
            val f = transformValueRegex.find(transformValue)!!
            val values = f.groupValues[1].split(',').map { it.toFloat() }

            // matrix(a, b, c, d, e, f)
            // ->
            // [a, c, e]
            // [b, d, f]
            // [0, 0, 1]
            val firstRow = floatArrayOf(values[0], values[2], values[4])
            val secondRow = floatArrayOf(values[1], values[3], values[5])

            Matrix3.from(arrayOf(firstRow, secondRow, floatArrayOf(0f, 0f, 1f)))
        } else {
            Matrix3.IDENTITY
        }
    }

    companion object {
        private val transformationPropertyNames = setOf("transform")
        private val number = Regex("""[-+]?(?:\d*\.\d+|\d+\.?)([eE][-+]?\d+)?""")
        private val transformValueRegex = Regex("""matrix\(((?:(?:${number.pattern})(?:,\s?)?){6})\)""")
    }
}
