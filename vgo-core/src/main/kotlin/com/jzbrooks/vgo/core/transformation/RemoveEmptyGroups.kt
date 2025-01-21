package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.util.math.Matrix3

/**
 * Remove unnecessary groups
 */
class RemoveEmptyGroups : BottomUpTransformer {
    override fun visit(graphic: Graphic) {
        removeEmptyGroups(graphic)
    }

    override fun visit(group: Group) {
        removeEmptyGroups(group)
    }

    override fun visit(clipPath: ClipPath) {}

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {}

    private fun removeEmptyGroups(containerElement: ContainerElement) {
        containerElement.elements =
            containerElement.elements.dropWhile { element ->
                element is Group && isEmpty(element)
            }
    }

    private fun isEmpty(group: Group): Boolean {
        if (group.elements.isEmpty() &&
            group.id == null &&
            group.transform.contentsEqual(Matrix3.IDENTITY) &&
            group.foreign.isEmpty()
        ) {
            return true
        }

        return group.elements.filterIsInstance<Group>().any(::isEmpty)
    }
}
