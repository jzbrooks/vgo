package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.util.math.Matrix3

/**
 * Collapse unnecessary nested groups into a single group
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Has been relocated to the transformation package",
    replaceWith = ReplaceWith("com.jzbrooks.vgo.core.transformation.CollapseGroups"),
)
class CollapseGroups : BottomUpOptimization {
    private val Group.isMergeable: Boolean
        get() {
            val hasValidClipPath = elements.any { it is ClipPath }
            val hasAttributes = id != null || !transform.contentsEqual(Matrix3.IDENTITY) || foreign.isNotEmpty()
            return !hasValidClipPath && elements.isNotEmpty() && !hasAttributes
        }

    override fun visit(graphic: Graphic) {
        mergeChildGroups(graphic)
    }

    override fun visit(group: Group) {
        mergeChildGroups(group)
    }

    override fun visit(clipPath: ClipPath) {}

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {}

    private fun mergeChildGroups(containerElement: ContainerElement) {
        containerElement.elements =
            containerElement.elements.flatMap {
                if (it is Group && it.isMergeable) it.elements else listOf(it)
            }
    }
}
