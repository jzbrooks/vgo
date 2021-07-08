package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ElementVisitor
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.util.math.Matrix3

/**
 * Apply transformations to paths command coordinates in a group
 */
class BakeTransformations : ElementVisitor, BottomUpOptimization {

    override fun visit(graphic: Graphic) {}
    override fun visit(clipPath: ClipPath) {}
    override fun visit(extra: Extra) {}
    override fun visit(path: Path) {}

    override fun visit(group: Group) {
        group.elements = group.elements.flatMap {
            if (it is Group && areElementsRelocatable(it)) it.elements
            else listOf(it)
        }

        val groupTransform = group.transform

        if (group.elements.any { it !is Path } || groupTransform.contentsEqual(Matrix3.IDENTITY))
            return

        for (child in group.elements) {
            (child as Path).applyTransform(groupTransform)
        }

        // Transform is baked. We don't want to apply it twice.
        group.transform = Matrix3.IDENTITY
    }

    private fun areElementsRelocatable(group: Group): Boolean {
        return group.id == null &&
            group.transform.contentsEqual(Matrix3.IDENTITY) &&
            group.foreign.isEmpty() &&
            group.elements.all { it is Path }
    }
}
