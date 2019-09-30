package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.*

class BakeTransformations : Optimization {
    override fun optimize(graphic: Graphic) {
        topDownVisit(graphic)
    }

    private fun topDownVisit(element: Element): Element {
        return when (element) {
            is Group -> {
                if (element.attributes.containsKey("scaleX")) {

                }
                element.apply { elements = elements.map(::topDownVisit) }
            }
            is ContainerElement -> {
                element.apply { elements = elements.map(::topDownVisit) }
            }
            else -> element
        }
    }

    private fun bakeIntoGroup(group: Group) {

    }

    private fun bakeIntoPath(path: Path) {

    }
}