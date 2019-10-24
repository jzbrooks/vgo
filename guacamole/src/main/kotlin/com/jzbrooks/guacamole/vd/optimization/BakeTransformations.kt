package com.jzbrooks.guacamole.vd.optimization

import com.jzbrooks.guacamole.core.graphic.*
import com.jzbrooks.guacamole.core.graphic.command.*
import com.jzbrooks.guacamole.core.optimization.Optimization
import com.jzbrooks.guacamole.core.util.math.Matrix3
import com.jzbrooks.guacamole.core.util.math.Vector3
import kotlin.math.*

class BakeTransformations : Optimization {
    override fun optimize(graphic: Graphic) {
        topDownVisit(graphic)
    }

    private fun topDownVisit(element: Element): Element {
        return when (element) {
            is ContainerElement -> {
                if (element is Group) {
                    bakeIntoGroup(element)
                }

                element.apply { elements = elements.map(::topDownVisit) }
            }
            else -> element
        }
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
                    child.commands = child.commands.map { command ->
                        applyTransforms(command, groupTransform)
                    }
                }
            }

            for (transformAttribute in groupTransforms) {
                group.attributes.remove(transformAttribute)
            }
        }
    }

    private fun applyTransforms(command: Command, transform: Matrix3): Command {
        return when (command) {
            is MoveTo -> {
                command.apply {
                    parameters = parameters.map {
                        (transform * Vector3(it)).toPoint()
                    }
                }
            }
            is LineTo -> {
                command.apply {
                    parameters = parameters.map {
                        (transform * Vector3(it)).toPoint()
                    }
                }
            }
            is HorizontalLineTo -> {
                command.apply {
                    parameters = parameters.map { x ->
                        val transformed = (transform * Vector3(x, 0f,1f))
                        transformed.i
                    }
                }
            }
            is VerticalLineTo -> {
                command.apply {
                    parameters = parameters.map { y ->
                        val transformed = (transform * Vector3(0f, y, 1f))
                        transformed.j
                    }
                }
            }
            is QuadraticBezierCurve -> {
                command.apply {
                    parameters.forEach {
                        it.control = (transform * Vector3(it.control)).toPoint()
                        it.end = (transform * Vector3(it.end)).toPoint()
                    }
                }
            }
            is ShortcutQuadraticBezierCurve -> {
                command.apply {
                    parameters = parameters.map {
                        (transform * Vector3(it)).toPoint()
                    }
                }
            }
            is CubicBezierCurve -> {
                command.apply {
                    parameters.forEach {
                        it.startControl = (transform * Vector3(it.startControl)).toPoint()
                        it.endControl = (transform * Vector3(it.endControl)).toPoint()
                        it.end = (transform * Vector3(it.end)).toPoint()
                    }
                }
            }
            is ShortcutCubicBezierCurve -> {
                command.apply {
                    parameters.forEach {
                        it.endControl = (transform * Vector3(it.endControl)).toPoint()
                        it.end = (transform * Vector3(it.end)).toPoint()
                    }
                }
            }
            is EllipticalArcCurve -> {
                command.apply {
                    parameters.forEach {
                        it.end = (transform * Vector3(it.end)).toPoint()
                    }
                }
            }
            else -> command
        }
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
                arrayOf(scaleX ?: 1f, 0f, 0f),
                arrayOf(0f, scaleY ?: 1f, 0f),
                arrayOf(0f, 0f, 1f)
        ))

        val translation = Matrix3.from(arrayOf(
                arrayOf(1f, 0f, translationX ?: 0f),
                arrayOf(0f, 1f, translationY ?: 0f),
                arrayOf(0f, 0f, 1f)
        ))

        val pivot = Matrix3.from(arrayOf(
                arrayOf(1f, 0f, pivotX ?: 0f),
                arrayOf(0f, 1f, pivotY ?: 0f),
                arrayOf(0f, 0f, 1f)
        ))

        val antiPivot = Matrix3.from(arrayOf(
                arrayOf(1f, 0f, (pivotX ?: 0f) * -1),
                arrayOf(0f, 1f, (pivotY ?: 0f) * -1),
                arrayOf(0f, 0f, 1f)
        ))

        val rotate = rotation?.let {
            val radians = it * PI.toFloat() / 180f
            Matrix3.from(arrayOf(
                    arrayOf(cos(radians), -sin(radians), 0f),
                    arrayOf(sin(radians), cos(radians), 0f),
                    arrayOf(0f, 0f, 1f)
            ))
        } ?: Matrix3.IDENTITY

        return listOf(pivot, translation, rotate, scale, antiPivot).reduce(Matrix3::times)
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