package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.*
import com.jzbrooks.guacamole.graphic.command.*
import com.jzbrooks.guacamole.util.math.Matrix3
import com.jzbrooks.guacamole.util.math.Vector3

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
        val groupTransform = group.transform
        if (groupTransform != null) {
            for(child in group.elements) {
                if (child is Group) {
                    child.transform = if (child.transform != null) {
                        val childTransform = child.transform!!
                        groupTransform * childTransform
                    } else {
                        groupTransform
                    }
                } else if (child is PathElement) {
                    child.commands = child.commands.map { command ->
                        applyTransforms(command, groupTransform)
                    }
                }
            }

            group.transform = null
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
}