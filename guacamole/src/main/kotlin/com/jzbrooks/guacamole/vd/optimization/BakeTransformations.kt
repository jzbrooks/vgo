package com.jzbrooks.guacamole.vd.optimization

import com.jzbrooks.guacamole.core.graphic.Group
import com.jzbrooks.guacamole.core.graphic.PathElement
import com.jzbrooks.guacamole.core.graphic.command.*
import com.jzbrooks.guacamole.core.optimization.GroupVisitor
import com.jzbrooks.guacamole.core.optimization.TopDownOptimization
import com.jzbrooks.guacamole.core.util.math.Matrix3
import com.jzbrooks.guacamole.core.util.math.Point
import com.jzbrooks.guacamole.core.util.math.Vector3
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BakeTransformations : TopDownOptimization, GroupVisitor {
    override fun visit(group: Group) {
        bakeIntoGroup(group)
    }

    private fun bakeIntoGroup(group: Group) {
        val groupTransforms = group.attributes.keys intersect transformationPropertyNames

        if (groupTransforms.isNotEmpty()) {
            val groupTransform = computeTransformationMatrix(group)

            for(child in group.elements) {
                if (child is Group) {
                    val childTransformations = child.attributes.keys intersect transformationPropertyNames
                    val shared = groupTransforms intersect childTransformations
                    val notShared = groupTransforms - childTransformations

                    for (transformKey in notShared) {
                        child.attributes[transformKey] = group.attributes.getValue(transformKey)
                    }

                    for (transformKey in shared) {
                        val childValue = child.attributes.getValue(transformKey).toFloat()
                        val groupValue = group.attributes.getValue(transformKey).toFloat()
                        child.attributes[transformKey] = when {
                            transformKey.startsWith("android:scale") -> (childValue * groupValue).toString()
                            transformKey == "android:rotation" -> ((childValue + groupValue) % 360).toString()
                            else -> (childValue + groupValue).toString()
                        }
                    }
                } else if (child is PathElement) {
                    applyTransform(child, groupTransform)
                }
            }

            for (transformAttribute in groupTransforms) {
                group.attributes.remove(transformAttribute)
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
                        parameters = parameters.map { parameter ->
                            transform(variant, parameter, currentPoint, transform)
                        }

                        currentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.last()
                        } else {
                            parameters.last()
                        }

                        subPathStart.push(currentPoint)
                    }
                }
                is LineTo -> {
                    command.apply {
                        parameters = parameters.map { parameter ->
                            transform(variant, parameter, currentPoint, transform)
                        }

                        currentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.last()
                        } else {
                            parameters.last()
                        }
                    }
                }
                is HorizontalLineTo -> {
                    command.apply {
                        parameters = parameters.map { x ->
                            transform(variant, Point(x, 0f), currentPoint, transform).x
                        }

                        currentPoint.x = if (variant == CommandVariant.RELATIVE) {
                            currentPoint.x + parameters.last()
                        } else {
                            parameters.last()
                        }
                    }
                }
                is VerticalLineTo -> {
                    command.apply {
                        parameters = parameters.map { y ->
                            transform(variant, Point(0f, y), currentPoint, transform).y
                        }

                        currentPoint.y = if (variant == CommandVariant.RELATIVE) {
                            currentPoint.y + parameters.last()
                        } else {
                            parameters.last()
                        }
                    }
                }
                is QuadraticBezierCurve -> {
                    command.apply {
                        parameters.forEach {
                            it.control = transform(variant, it.control, currentPoint, transform)
                            it.end = transform(variant, it.end, currentPoint, transform)
                        }

                        currentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.last().end
                        } else {
                            parameters.last().end
                        }
                    }
                }
                is ShortcutQuadraticBezierCurve -> {
                    command.apply {
                        parameters = parameters.map { parameter ->
                            transform(variant, parameter, currentPoint, transform)
                        }

                        currentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.last()
                        } else {
                            parameters.last()
                        }
                    }
                }
                is CubicBezierCurve -> {
                    command.apply {
                        parameters.forEach {
                            it.startControl = transform(variant, it.startControl, currentPoint, transform)
                            it.endControl = transform(variant, it.endControl, currentPoint, transform)
                            it.end = transform(variant, it.end, currentPoint, transform)
                        }

                        currentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.last().end
                        } else {
                            parameters.last().end
                        }
                    }
                }
                is ShortcutCubicBezierCurve -> {
                    command.apply {
                        parameters.forEach {
                            it.endControl = transform(variant, it.endControl, currentPoint, transform)
                            it.end = transform(variant, it.end, currentPoint, transform)
                        }

                        currentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.last().end
                        } else {
                            parameters.last().end
                        }
                    }
                }
                is EllipticalArcCurve -> {
                    command.apply {
                        parameters.forEach {
                            it.end = transform(variant, it.end, currentPoint, transform)
                        }

                        currentPoint = if (variant == CommandVariant.RELATIVE) {
                            currentPoint + parameters.last().end
                        } else {
                            parameters.last().end
                        }
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

    private fun transform(
            variant: CommandVariant,
            point: Point,
            currentPoint: Point,
            transform: Matrix3
    ): Point {
        val vector = if (variant == CommandVariant.RELATIVE) {
            Vector3(currentPoint + point)
        } else {
            Vector3(point)
        }

        var result = (transform * vector).toPoint()

        if (variant == CommandVariant.RELATIVE) {
            result -= (transform * Vector3(currentPoint)).toPoint()
        }

        return result
    }

    private fun computeTransformationMatrix(group: Group): Matrix3 {
        val scaleX = group.attributes["android:scaleX"]?.toFloat()
        val scaleY = group.attributes["android:scaleY"]?.toFloat()

        val translationX = group.attributes["android:translateX"]?.toFloat()
        val translationY = group.attributes["android:translateY"]?.toFloat()

        val pivotX = group.attributes["android:pivotX"]?.toFloat()
        val pivotY = group.attributes["android:pivotY"]?.toFloat()

        val rotation = group.attributes["android:rotation"]?.toFloat()

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