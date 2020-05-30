package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import kotlin.math.sign

/**
 * Convert lines into shorter commands where possible
 */
class SimplifyLineCommands(private val tolerance: Float) : TopDownOptimization, PathElementVisitor {
    lateinit var commands: MutableList<Command>
    override fun visit(pathElement: PathElement) {
        commands = mutableListOf()

        if (pathElement.commands.isNotEmpty()) {
            commands.add((pathElement.commands.first() as MoveTo).copy())
            for (command in pathElement.commands.drop(1)) {

                assert((command as? ParameterizedCommand<*>)?.variant != CommandVariant.ABSOLUTE)
                assert((command as? HorizontalLineTo)?.parameters?.size ?: 0 < 2)
                assert((command as? VerticalLineTo)?.parameters?.size ?: 0 < 2)

                val processedCommand = process(command)
                if (processedCommand != null) {
                    commands.add(processedCommand)
                }
            }
        }

        pathElement.commands = commands
    }

    private fun process(command: Command): Command? {
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
            is HorizontalLineTo -> {
                val lastAdded = commands.last()
                when {
                    lastAdded is HorizontalLineTo && lastAdded.parameters.last().sign == command.parameters.last().sign -> {
                        lastAdded.parameters = listOf(lastAdded.parameters.last() + command.parameters.last())
                        null
                    }
                    else -> command
                }
            }
            is VerticalLineTo -> {
                val lastAdded = commands.last()
                when {
                    lastAdded is VerticalLineTo && lastAdded.parameters.last().sign == command.parameters.last().sign -> {
                        lastAdded.parameters = listOf(lastAdded.parameters.last() + command.parameters.last())
                        null
                    }
                    else -> command
                }
            }
            else -> command
        }
    }
}