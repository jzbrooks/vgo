package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.util.math.Point

/**
 * Elide commands that don't contribute to the overall graphic
 */
class RemoveRedundantCommands : TopDownOptimization, PathElementVisitor {
    override fun visit(pathElement: PathElement) {
        val commandCount = pathElement.commands.size

        if (commandCount > 0) {
            val commands = mutableListOf<Command>((pathElement.commands.first() as MoveTo).copy())
            loop@ for (current in pathElement.commands.drop(1)) {
                assert((current as? ParameterizedCommand<*>)?.variant != CommandVariant.ABSOLUTE)

                when (current) {
                    is LineTo -> if (current.parameters.all { it == Point.ZERO }) { continue@loop }
                    is VerticalLineTo -> if (current.parameters.all { it == 0.0f }) { continue@loop }
                    is HorizontalLineTo -> if (current.parameters.all { it == 0.0f }) { continue@loop }
                    is CubicBezierCurve -> if (current.parameters.all { it == CubicBezierCurve.Parameter(Point(0f, 0f), Point(0f, 0f), Point(0f, 0f)) }) { continue@loop }
                    is ShortcutCubicBezierCurve -> if (current.parameters.all { it == ShortcutCubicBezierCurve.Parameter(Point(0f, 0f), Point(0f, 0f)) }) { continue@loop }
                    is QuadraticBezierCurve -> if (current.parameters.all { it == QuadraticBezierCurve.Parameter(Point(0f, 0f), Point(0f, 0f)) }) { continue@loop }
                    is ShortcutQuadraticBezierCurve -> if (current.parameters.all { it == Point.ZERO }) { continue@loop }
                }

                commands.add(current)
            }

            pathElement.commands = commands
        }
    }
}
