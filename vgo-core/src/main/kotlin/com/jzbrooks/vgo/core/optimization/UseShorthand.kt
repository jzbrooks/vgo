package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant

class UseShorthand : TopDownOptimization, PathElementVisitor {
    override fun visit(pathElement: PathElement) {
        val commandCount = pathElement.commands.size

        if (commandCount > 0) {
            val commands = mutableListOf<Command>((pathElement.commands.first() as MoveTo).copy())
            loop@ for (current in pathElement.commands.slice(1 until commandCount)) {
                assert((current as? ParameterizedCommand<*>)?.variant != CommandVariant.ABSOLUTE)

                val lastAdded = commands.last() as? ParameterizedCommand<*>

                if (current is CubicBezierCurve) {
                    val currentFinalParameter = current.parameters.last()
                    if (lastAdded is CubicBezierCurve &&
                            currentFinalParameter.startControl == (lastAdded.parameters.last().run { end - endControl })) {
                        commands.add(ShortcutCubicBezierCurve(current.variant, current.parameters.map {
                            ShortcutCubicBezierCurve.Parameter(it.endControl, it.end)
                        }))
                        continue@loop
                    }
                }
                commands.add(current)
            }

            pathElement.commands = commands
        }
    }
}