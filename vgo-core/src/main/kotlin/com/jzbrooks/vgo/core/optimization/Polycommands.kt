package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*

/**
 * Polycommands can sometimes provide an
 * opportunity to save space in a path element.
 *
 * For example, when a command sequence is l0,0l-1,1,
 * because the second coordinate pair begins with a negative
 * number, you can omit the separator between it and the preceeding
 * pair. It becomes 10,0-1,1.
 */
class Polycommands : TopDownOptimization, PathElementVisitor {
    override fun visit(pathElement: PathElement) {
        val commandCount = pathElement.commands.size

        if (commandCount > 0) {
            val commands = mutableListOf<Command>((pathElement.commands.first() as MoveTo).copy())
            loop@ for (current in pathElement.commands.drop(1)) {
                val currentParam = current as? ParameterizedCommand<*>
                val lastAdded = commands.last() as? ParameterizedCommand<*>
                if (lastAdded?.variant == currentParam?.variant) {
                    when {
                        lastAdded is MoveTo && current is LineTo -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is LineTo && current is LineTo -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is VerticalLineTo && current is VerticalLineTo -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is HorizontalLineTo && current is HorizontalLineTo -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is CubicBezierCurve && current is CubicBezierCurve -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is ShortcutCubicBezierCurve && current is ShortcutCubicBezierCurve -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is QuadraticBezierCurve && current is QuadraticBezierCurve -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is ShortcutQuadraticBezierCurve && current is ShortcutQuadraticBezierCurve -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                    }
                }
                commands.add(current)
            }

            pathElement.commands = commands
        }
    }
}