package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.ContainerElement
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.PathElement

class RemoveRedundantCommands : Optimization {
    override fun optimize(graphic: Graphic) {
        topDownOptimize(graphic)
    }

    private fun topDownOptimize(element: Element): Element {
        return when (element) {
            is PathElement -> removeRedundantCommands(element)
            is ContainerElement -> element.apply { elements = element.elements.map(::topDownOptimize) }
            else -> element
        }
    }

    private fun removeRedundantCommands(pathElement: PathElement): PathElement {
        val commandCount = pathElement.commands.size

        if (commandCount > 0) {
            val commands = mutableListOf(pathElement.commands.first())
            for (current in pathElement.commands.slice(1 until commandCount)) {
                val lastAdded = commands.last()

                // todo: might break the simple heart
                if (lastAdded != current) commands.add(current)
            }

            pathElement.commands = commands
        }

        return pathElement
    }
}
