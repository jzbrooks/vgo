package com.jzbrooks.guacamole.svg.optimization

import com.jzbrooks.guacamole.core.graphic.*
import com.jzbrooks.guacamole.core.graphic.command.*
import com.jzbrooks.guacamole.core.optimization.GroupVisitor
import com.jzbrooks.guacamole.core.optimization.Optimization
import com.jzbrooks.guacamole.core.optimization.TopDownOptimization
import com.jzbrooks.guacamole.core.util.math.Matrix3
import com.jzbrooks.guacamole.core.util.math.Vector3
import kotlin.math.*

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
                for(child in group.elements) {
                    if (child is PathElement) {
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
        private val number = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)([eE][-+]?\\d+)?")
        private val transformValueRegex = Regex("matrix\\(((?:(?:${number.pattern})(?:,\\s?)?){6})\\)")
    }
}