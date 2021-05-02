package com.jzbrooks.vgo.vd.optimization

import com.jzbrooks.vgo.core.graphic.*
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.optimization.Optimization
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.Vector3
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
            element is Group && element.attributes.foreign.values.none { it.startsWith("@") } -> {
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
        val groupTransforms = group.attributes.foreign.keys intersect transformationPropertyNames

        if (groupTransforms.isNotEmpty()) {
            val groupTransform = computeTransformationMatrix(group)

            val children = mutableListOf<Element>()
            for(child in group.elements) {
                if (child is Group) {
                    val childTransformations = child.attributes.foreign.keys intersect transformationPropertyNames
                    val shared = groupTransforms intersect childTransformations
                    val notShared = groupTransforms - childTransformations

                    for (transformKey in notShared) {
                        child.attributes.foreign[transformKey] = group.attributes.foreign.getValue(transformKey)
                    }

                    for (transformKey in shared) {
                        val groupValue = group.attributes.foreign.getValue(transformKey).toFloat()
                        val childValue = child.attributes.foreign.getValue(transformKey).toFloatOrNull()

                        // If the child has a resource-specified value, then
                        // merging isn't possible. Wrap the child in a new group with the
                        // current group transform value and proceed to bake siblings in
                        // case this child had other path element siblings.
                        if (childValue == null) {
                            val transforms = child.attributes.foreign.filterKeys{childTransformations.contains(it)}
                            for ((transform) in transforms) {
                                child.attributes.foreign.remove(transform)
                            }

                            children.add(Group(listOf(child), Group.Attributes(null, transforms.toMutableMap())))
                            continue
                        }

                        child.attributes.foreign[transformKey] = when {
                            transformKey.startsWith("android:scale") -> (childValue * groupValue).toString()
                            transformKey == "android:rotation" -> ((childValue + groupValue) % 360).toString()
                            else -> (childValue + groupValue).toString()
                        }
                        children.add(child)
                    }
                } else if (child is PathElement) {
                    applyTransform(child, groupTransform)
                    children.add(child)
                }
            }

            group.elements = children

            for (transformAttribute in groupTransforms) {
                group.attributes.foreign.remove(transformAttribute)
            }
        }
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

    private fun computeTransformationMatrix(group: Group): Matrix3 {
        val scaleX = group.attributes.foreign["android:scaleX"]?.toFloat()
        val scaleY = group.attributes.foreign["android:scaleY"]?.toFloat()

        val translationX = group.attributes.foreign["android:translateX"]?.toFloat()
        val translationY = group.attributes.foreign["android:translateY"]?.toFloat()

        val pivotX = group.attributes.foreign["android:pivotX"]?.toFloat()
        val pivotY = group.attributes.foreign["android:pivotY"]?.toFloat()

        val rotation = group.attributes.foreign["android:rotation"]?.toFloat()

        val scale = Matrix3.from(arrayOf(
                floatArrayOf(scaleX ?: 1f, 0f, 0f),
                floatArrayOf(0f, scaleY ?: 1f, 0f),
                floatArrayOf(0f, 0f, 1f)
        ))

        val translation = Matrix3.from(arrayOf(
                floatArrayOf(1f, 0f, translationX ?: 0f),
                floatArrayOf(0f, 1f, translationY ?: 0f),
                floatArrayOf(0f, 0f, 1f)
        ))

        val pivot = Matrix3.from(arrayOf(
                floatArrayOf(1f, 0f, pivotX ?: 0f),
                floatArrayOf(0f, 1f, pivotY ?: 0f),
                floatArrayOf(0f, 0f, 1f)
        ))

        val pivotInverse = Matrix3.from(arrayOf(
                floatArrayOf(1f, 0f, (pivotX ?: 0f) * -1),
                floatArrayOf(0f, 1f, (pivotY ?: 0f) * -1),
                floatArrayOf(0f, 0f, 1f)
        ))

        val rotate = rotation?.let {
            val radians = it * PI.toFloat() / 180f
            Matrix3.from(arrayOf(
                    floatArrayOf(cos(radians), -sin(radians), 0f),
                    floatArrayOf(sin(radians), cos(radians), 0f),
                    floatArrayOf(0f, 0f, 1f)
            ))
        } ?: Matrix3.IDENTITY

        return listOf(pivot, translation, rotate, scale, pivotInverse).reduce(Matrix3::times)
    }

    companion object {
        private val transformationPropertyNames = setOf(
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