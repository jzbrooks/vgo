package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo

/**
 * Polycommands can sometimes provide an
 * opportunity to save space in a path element.
 *
 * For example, when a command sequence is l0,0l-1,1,
 * because the second coordinate pair begins with a negative
 * number, you can omit the separator between it and the preceeding
 * pair. It becomes 10,0-1,1.
 */
class Polycommands : TopDownOptimization {
    override fun visit(graphic: Graphic) {}
    override fun visit(clipPath: ClipPath) {}
    override fun visit(group: Group) {}
    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {
        val commandCount = path.commands.size

        if (commandCount > 0) {
            val commands = mutableListOf<Command>((path.commands.first() as MoveTo).copy())
            loop@ for (current in path.commands.drop(1)) {
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
                        lastAdded is SmoothCubicBezierCurve && current is SmoothCubicBezierCurve -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is QuadraticBezierCurve && current is QuadraticBezierCurve -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                        lastAdded is SmoothQuadraticBezierCurve && current is SmoothQuadraticBezierCurve -> {
                            lastAdded.parameters += current.parameters
                            continue@loop
                        }
                    }
                }
                commands.add(current)
            }

            path.commands = commands
        }
    }
}
