package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic
import com.jzbrooks.guacamole.graphic.Group

class RemoveEmptyGroups : Optimization {
    override fun optimize(graphic: Graphic) {
        graphic.elements = graphic.elements.asSequence()
                .dropWhile { item -> item is Group && isEmpty(item) }
                .toList()
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