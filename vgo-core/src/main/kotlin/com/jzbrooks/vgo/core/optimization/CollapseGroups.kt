package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.util.math.Matrix3

/**
 * Collapse unnecessary nested groups into a single group
 */
class CollapseGroups : BottomUpOptimization, ContainerElementVisitor {

    private val Group.isMergeable: Boolean
        get() {
            val hasValidClipPath = elements.any { it is ClipPath }
            val hasAttributes = id != null || !transform.contentsEqual(Matrix3.IDENTITY) || foreign.isNotEmpty()
            return !hasValidClipPath && elements.isNotEmpty() && !hasAttributes
        }

    override fun visit(containerElement: ContainerElement) {
        containerElement.elements = containerElement.elements.flatMap {
            if (it is Group && it.isMergeable) it.elements else listOf(it)
        }
    }
}
