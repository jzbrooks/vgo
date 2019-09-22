package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic
import com.jzbrooks.guacamole.graphic.Group

class RemoveEmptyGroups : Optimization {
    override fun visit(graphic: Graphic) {
        val elements = mutableListOf<Element>()

        for (item in graphic.elements) {
            if (item is Group && isEmpty(item)) {
                continue
            } else {
                elements.add(item)
            }
        }

        graphic.elements = elements
    }

    private fun isEmpty(group: Group): Boolean {
        if (group.elements.isEmpty() && group.attributes.isEmpty()) return true

        for (subgroup in group.elements.filterIsInstance<Group>()) {
            if (isEmpty(subgroup)) {
                return true
            }
        }

        return false
    }
}