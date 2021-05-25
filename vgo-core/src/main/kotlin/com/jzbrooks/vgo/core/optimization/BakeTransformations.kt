package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.applyTransform
import com.jzbrooks.vgo.core.util.math.Matrix3

/**
 * Apply transformations to paths command coordinates in a group
 */
class BakeTransformations : ContainerElementVisitor, BottomUpOptimization {

    override fun visit(containerElement: ContainerElement) {
        if (containerElement is Group) bakeGroups(containerElement)
    }

    private fun bakeGroups(containerElement: ContainerElement) {
        containerElement.elements = containerElement.elements.flatMap {
            if (it is Group && areElementsRelocatable(it)) it.elements
            else listOf(it)
        }

        if (containerElement is Group) {
            val groupTransform = containerElement.transform

            if (containerElement.elements.any { it !is PathElement } || groupTransform == Matrix3.IDENTITY) return

            for (child in containerElement.elements) {
                (child as PathElement).applyTransform(groupTransform)
            }

            // Transform is baked. We don't want to apply it twice.
            containerElement.transform = Matrix3.IDENTITY
        }
    }

    private fun areElementsRelocatable(group: Group): Boolean {
        return group.id == null &&
                group.transform === Matrix3.IDENTITY &&
                group.foreign.isEmpty() &&
                group.elements.all { it is PathElement }
    }
}
