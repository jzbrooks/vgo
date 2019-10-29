package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.ContainerElement
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.PathElement
import com.jzbrooks.guacamole.core.graphic.command.*

class SimplifyLineCommands(private val tolerance: Float) : Optimization {
    override fun optimize(graphic: Graphic) {
        topDownOptimize(graphic)
    }

    private fun topDownOptimize(element: Element): Element {
        return when (element) {
            is PathElement -> element.apply { commands = element.commands.map(::process) }
            is ContainerElement -> element.apply { elements = element.elements.map(::topDownOptimize) }
            else -> element
        }
    }

    private fun process(command: Command): Command {
        return when (command) {
            is LineTo -> {
                val firstParameter = command.parameters.first()
                when {
                    command.parameters.size > 1 -> command
                    firstParameter.x in -tolerance..tolerance -> VerticalLineTo(command.variant, listOf(firstParameter.y))
                    firstParameter.y in -tolerance..tolerance -> HorizontalLineTo(command.variant, listOf(firstParameter.x))
                    else -> command
                }
            }
            // todo: convert straight curves and arcs
            else -> command
        }
    }
}