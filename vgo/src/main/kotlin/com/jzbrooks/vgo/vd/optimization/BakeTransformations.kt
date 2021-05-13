package com.jzbrooks.vgo.vd.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
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
import com.jzbrooks.vgo.core.optimization.Optimization
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.Vector3
import java.util.Stack

/**
 * Apply transformations to paths command coordinates in a group
 * and remove the transformations from the group
 */
class BakeTransformations : Optimization {

    override fun optimize(graphic: Graphic) {
        graphic.elements = graphic.elements.map(::topDownTraverse)
    }

    private fun topDownTraverse(element: Element): Element {
        return when {
            element is Group && element.foreign.values.none { it.startsWith("@") } -> {
                bakeIntoGroup(element)
                element.apply { elements.map(::topDownTraverse) }
            }
            element is ContainerElement -> {
                element.apply { elements.map(::topDownTraverse) }
            }
            else -> element
        }
    }

    private fun bakeIntoGroup(group: Group) {
        val groupTransform = group.transform

        val children = mutableListOf<Element>()
        for (child in group.elements) {
            if (child is Group) {
                val childTransform = child.transform
                val childForeignTransformations = child.foreign.filterKeys(TRANSFORM_KEYS::contains)

                if (childForeignTransformations.isEmpty()) {
                    child.transform = groupTransform * childTransform
                } else {
                    // If the child has a foreign transform value (usually this means non-literal),
                    // then merging isn't possible. Wrap the child in a new group with the
                    // current group transform value and proceed to bake siblings in
                    // case this child had other path element siblings.

                    for ((transform) in childForeignTransformations) {
                        child.foreign.remove(transform)
                    }

                    val syntheticGroup = Group(
                        listOf(child),
                        null,
                        childForeignTransformations.toMutableMap(),
                        groupTransform * childTransform,
                    )

                    children.add(syntheticGroup)
                }
            } else if (child is PathElement) {

                if (groupTransform !== Matrix3.IDENTITY) {
                    applyTransform(child, groupTransform)
                }

                children.add(child)
            }
        }

        // Transform is baked. We don't want to apply it twice.
        group.transform = Matrix3.IDENTITY

        group.elements = children
    }

    private fun applyTransform(element: PathElement, transform: Matrix3) {
        if (element.commands.isEmpty()) return

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

    companion object {
        private val TRANSFORM_KEYS = hashSetOf(
            "android:scaleX",
            "android:scaleY",
            "android:translateX",
            "android:translateY",
            "android:pivotX",
            "android:pivotY",
            "android:rotation"
        )
    }
}
