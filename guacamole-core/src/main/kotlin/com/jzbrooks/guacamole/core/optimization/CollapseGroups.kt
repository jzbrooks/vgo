package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.ContainerElement
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.Group

class CollapseGroups : Optimization {
    override fun optimize(graphic: Graphic) {
        bottomUpVisit(graphic)
    }

    private fun bottomUpVisit(element: Element): Element {
        if (element !is ContainerElement) return element

        val newElements = mutableListOf<Element>()
        for (child in element.elements) {
            val parent = bottomUpVisit(child)
            if (parent is Group && parent.elements.isNotEmpty() && parent.attributes.isEmpty()) {
                newElements.addAll(parent.elements)
            } else {
                newElements.add(parent)
            }
        }

        return element.apply { elements = newElements }
    }
}
