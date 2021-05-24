package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.applyTransform
import com.jzbrooks.vgo.core.util.math.Matrix3

/**
 * Apply transformations to paths command coordinates in a group
 */
class BakeTransformations(private val transformKeys: HashSet<String>) : GroupVisitor, BottomUpOptimization {

    override fun visit(group: Group) = bakeIntoGroup(group)

    private fun bakeIntoGroup(group: Group) {
        val groupTransform = group.transform

        if (group.elements.any { it !is PathElement } || groupTransform == Matrix3.IDENTITY) return

        for (child in group.elements) {
            (child as PathElement).applyTransform(groupTransform)
        }

        // Transform is baked. We don't want to apply it twice.
        group.transform = Matrix3.IDENTITY
    }
}
