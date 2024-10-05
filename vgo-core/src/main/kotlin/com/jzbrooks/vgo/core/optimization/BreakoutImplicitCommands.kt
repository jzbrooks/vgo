package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo

/**
 * Enables more resolution in the other command
 * related optimizations like [CommandVariant] and [RemoveRedundantCommands]
 */
class BreakoutImplicitCommands : TopDownOptimization {
    override fun visit(graphic: Graphic) {}

    override fun visit(clipPath: ClipPath) {}

    override fun visit(group: Group) {}

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {
        val commands = mutableListOf<Command>()

        for (current in path.commands) {
            if (current is ParameterizedCommand<*> && current.parameters.size > 1) {
                val splitCommands = divideParameters(current)
                commands.addAll(splitCommands)
            } else {
                commands.add(current)
            }
        }

        path.commands = commands
    }

    private fun divideParameters(first: ParameterizedCommand<*>): List<Command> =
        when (first) {
            is MoveTo ->
                first.parameters.mapIndexed { i, it ->
                    if (i == 0) first.copy(parameters = listOf(it)) else LineTo(first.variant, listOf(it))
                }
            is LineTo -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is SmoothQuadraticBezierCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is HorizontalLineTo -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is VerticalLineTo -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is QuadraticBezierCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is CubicBezierCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is SmoothCubicBezierCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is EllipticalArcCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
        }
}
