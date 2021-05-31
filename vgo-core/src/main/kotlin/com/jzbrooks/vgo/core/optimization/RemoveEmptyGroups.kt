package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.util.math.Matrix3

/**
 * Remove unnecessary groups
 */
class RemoveEmptyGroups : BottomUpOptimization, ContainerElementVisitor {
    override fun visit(containerElement: ContainerElement) {
        containerElement.elements = containerElement.elements.dropWhile { element ->
            element is Group && isEmpty(element)
        }
    }

    private fun isEmpty(group: Group): Boolean {
        if (group.elements.isEmpty() &&
            group.id == null &&
            group.transform.contentsEqual(Matrix3.IDENTITY) &&
            group.foreign.isEmpty()
        ) return true

        return group.elements.filterIsInstance<Group>().any(::isEmpty)
    }
}
