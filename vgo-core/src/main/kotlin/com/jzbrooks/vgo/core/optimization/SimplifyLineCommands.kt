package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import kotlin.math.sign

/**
 * Convert lines into shorter commands where possible
 */
class SimplifyLineCommands(
    private val tolerance: Float,
) : TopDownOptimization {
    lateinit var commands: MutableList<Command>

    override fun visit(graphic: Graphic) {}

    override fun visit(clipPath: ClipPath) {}

    override fun visit(group: Group) {}

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {
        commands = mutableListOf()

        if (path.commands.isNotEmpty()) {
            commands.add((path.commands.first() as MoveTo).copy())
            for (command in path.commands.drop(1)) {
                assert((command as? ParameterizedCommand<*>)?.variant != CommandVariant.ABSOLUTE)
                assert((command as? HorizontalLineTo)?.parameters?.size ?: 0 < 2)
                assert((command as? VerticalLineTo)?.parameters?.size ?: 0 < 2)

                val processedCommand = process(command)
                if (processedCommand != null) {
                    commands.add(processedCommand)
                }
            }
        }

        path.commands = commands
    }

    private fun process(command: Command): Command? =
        when (command) {
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
