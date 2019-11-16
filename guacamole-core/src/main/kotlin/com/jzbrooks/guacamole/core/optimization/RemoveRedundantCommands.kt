package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.PathElement
import com.jzbrooks.guacamole.core.graphic.command.Command
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.graphic.command.MoveTo
import com.jzbrooks.guacamole.core.graphic.command.ParameterizedCommand

class RemoveRedundantCommands : TopDownOptimization, PathElementVisitor {
    override fun visit(pathElement: PathElement) {
        val commandCount = pathElement.commands.size

        if (commandCount > 0) {
            val commands = mutableListOf<Command>((pathElement.commands.first() as MoveTo).copy())
            for (current in pathElement.commands.slice(1 until commandCount)) {
                val lastAdded = commands.last() as? ParameterizedCommand<*>

                if (lastAdded?.variant == CommandVariant.RELATIVE || lastAdded != current) {
                    commands.add(current)
                }
            }

            pathElement.commands = commands
        }
    }
}
