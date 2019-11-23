package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.PathElement
import com.jzbrooks.guacamole.core.graphic.command.Command
import com.jzbrooks.guacamole.core.graphic.command.HorizontalLineTo
import com.jzbrooks.guacamole.core.graphic.command.LineTo
import com.jzbrooks.guacamole.core.graphic.command.VerticalLineTo

class SimplifyLineCommands(private val tolerance: Float) : TopDownOptimization, PathElementVisitor {
    override fun visit(pathElement: PathElement) {
        pathElement.commands = pathElement.commands.map(::process)
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