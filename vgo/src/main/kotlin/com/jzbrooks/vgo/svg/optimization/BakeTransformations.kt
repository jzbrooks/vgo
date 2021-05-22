package com.jzbrooks.vgo.svg.optimization

import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.applyTransform
import com.jzbrooks.vgo.core.optimization.GroupVisitor
import com.jzbrooks.vgo.core.optimization.TopDownOptimization
import com.jzbrooks.vgo.core.util.math.Matrix3

/**
 * Apply transformations to paths command coordinates in a group
 * and remove the transformations from the group
 */
class BakeTransformations : TopDownOptimization, GroupVisitor {
    override fun visit(group: Group) {
        bakeIntoGroup(group)
    }

    private fun bakeIntoGroup(group: Group) {
        val groupTransform = group.transform

        // We can only do transform baking if everything in the group can be transform baked
        // todo: handle baking nested groups
        if (groupTransform != Matrix3.IDENTITY && group.elements.all { it is PathElement }) {
            for (child in group.elements) {
                if (child is PathElement) {
                    child.applyTransform(groupTransform)
                }
            }

            // Transforms are baked. Don't apply them again when rendering.
            group.transform = Matrix3.IDENTITY
        }
    }
}
