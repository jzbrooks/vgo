package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group

/**
 * Remove unnecessary groups
 */
class RemoveEmptyGroups : Optimization {
    override fun optimize(graphic: Graphic) {
        graphic.elements = graphic.elements.dropWhile { element ->
            element is Group && isEmpty(element)
        }
    }

    private fun isEmpty(group: Group): Boolean {
        if (group.elements.isEmpty() && group.attributes.isEmpty()) return true

        return group.elements.filterIsInstance<Group>().any(::isEmpty)
    }
}
