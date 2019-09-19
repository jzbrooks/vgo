package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.ContainerElement
import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Group

class RemoveEmptyGroups : Optimization<ContainerElement> {
    override fun visit(element: ContainerElement) {
        val elements = mutableListOf<Element>()

        for (item in element.elements) {
            if (item is Group && isEmpty(item)) {
                continue
            } else {
                elements.add(item)
            }
        }

        element.elements = elements
    }

    private fun isEmpty(group: Group): Boolean {
        if (group.elements.isEmpty() && group.metadata.isEmpty()) return true

        for (subgroup in group.elements.filterIsInstance<Group>()) {
            if (isEmpty(subgroup)) {
                return true
            }
        }

        return false
    }
}