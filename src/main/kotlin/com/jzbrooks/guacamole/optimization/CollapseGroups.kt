package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.ContainerElement
import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic
import com.jzbrooks.guacamole.graphic.Group

class CollapseGroups : Optimization {
    override fun visit(graphic: Graphic) {
        bottomUpVisit(graphic)
    }

    private fun bottomUpVisit(element: Element): Element {
        if (element !is ContainerElement) return element

        val newElements = mutableListOf<Element>()
        for (child in element.elements) {
            val foo = bottomUpVisit(child)
            if (foo is Group && foo.elements.isNotEmpty() && foo.attributes.isEmpty()) {
                newElements.addAll(foo.elements)
            } else {
                newElements.add(foo)
            }
        }

        return element.apply { elements = newElements }
    }
}
