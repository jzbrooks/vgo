package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.ContainerElement
import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic
import com.jzbrooks.guacamole.graphic.Group

class CollapseGroups : Optimization<Graphic> {
    override fun visit(element: Graphic) {
        bottomUpVisit(element)
    }

    private fun bottomUpVisit(element: Element): Element {
        if (element !is ContainerElement) return element

        val newElements = mutableListOf<Element>()
        for (child in element.elements) {
            val foo = bottomUpVisit(child)
            if (foo is Group && foo.elements.isNotEmpty() && foo.metadata.isEmpty()) {
                newElements.addAll(foo.elements)
            } else {
                newElements.add(foo)
            }
        }

        return element.apply { elements = newElements }
    }
}
