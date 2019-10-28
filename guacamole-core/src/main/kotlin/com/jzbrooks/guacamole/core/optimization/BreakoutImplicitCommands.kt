package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.ContainerElement
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.PathElement
import com.jzbrooks.guacamole.core.graphic.command.*

/**
 * Enables more resolution in the the other command
 * related optimizations like [CommandVariant] and [RemoveRedundantCommands]
 */
class BreakoutImplicitCommands : Optimization {
    override fun optimize(graphic: Graphic) {
        topDownOptimize(graphic)
    }

    private fun topDownOptimize(element: Element): Element {
        return when (element) {
            is PathElement -> process(element)
            is ContainerElement -> element.apply { elements = element.elements.map(::topDownOptimize) }
            else -> element
        }
    }

    private fun process(pathElement: PathElement): PathElement {
        val commands = mutableListOf<Command>()
        for (current in pathElement.commands) {
            if (current is ParameterizedCommand<*> && current.parameters.size > 1) {
                val splitCommands = divideParameters(current)
                commands.addAll(splitCommands)
            } else {
                commands.add(current)
            }
        }

        return pathElement.apply { this.commands = commands }
    }

    private fun divideParameters(first: ParameterizedCommand<*>) : List<Command> {
        return when(first) {
            is MoveTo -> first.parameters.mapIndexed { i, it ->
                if (i == 0) first.copy(parameters = listOf(it)) else LineTo(first.variant, listOf(it))
            }
            is LineTo -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is ShortcutQuadraticBezierCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is HorizontalLineTo -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is VerticalLineTo -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is QuadraticBezierCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is CubicBezierCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is ShortcutCubicBezierCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            is EllipticalArcCurve -> first.parameters.map { first.copy(parameters = listOf(it)) }
            else -> throw IllegalArgumentException("Cannot divide parameters for command type ${first::class}")
        }
    }
}
