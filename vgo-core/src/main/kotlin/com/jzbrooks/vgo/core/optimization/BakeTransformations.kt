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

        val children = mutableListOf<Element>()
        for (child in group.elements) {
            if (child is Group) {
                val childTransform = child.transform
                val childForeignTransformations = child.foreign.filterKeys(transformKeys::contains)

                if (childForeignTransformations.isEmpty()) {
                    child.transform = groupTransform * childTransform
                } else {
                    // If the child has a foreign transform value (usually this means non-literal),
                    // then merging isn't possible. Wrap the child in a new group with the
                    // current group transform value and proceed to bake siblings in
                    // case this child had other path element siblings.

                    for ((transform) in childForeignTransformations) {
                        child.foreign.remove(transform)
                    }

                    val syntheticGroup = Group(
                        listOf(child),
                        null,
                        childForeignTransformations.toMutableMap(),
                        groupTransform * childTransform,
                    )

                    children.add(syntheticGroup)
                }
            } else if (child is PathElement) {

                if (groupTransform !== Matrix3.IDENTITY) {
                    child.applyTransform(groupTransform)
                }

                children.add(child)
            }
        }

        // Transform is baked. We don't want to apply it twice.
        group.transform = Matrix3.IDENTITY

        group.elements = children
    }
}
