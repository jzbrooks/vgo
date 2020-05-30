package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.*

/**
 * Collapse unnecessary nested groups into a single group
 */
class CollapseGroups : Optimization {

    // Groups play a serious role in clip paths for
    // Android Vector Drawables. Perhaps this should
    // eventually be a target-specific optimization
    private val Group.isMergeable: Boolean
        get() {
            val hasValidClipPath = elements.any { it is PathElement && it !is Path }
            return !hasValidClipPath && elements.isNotEmpty() && attributes.isEmpty()
        }

    override fun optimize(graphic: Graphic) {
        bottomUpVisit(graphic)
    }

    private fun bottomUpVisit(element: Element): Element {
        if (element !is ContainerElement) return element

        val newElements = mutableListOf<Element>()
        for (child in element.elements) {
            val parent = bottomUpVisit(child)
            if (parent is Group && parent.isMergeable) {
                newElements.addAll(parent.elements)
            } else {
                newElements.add(parent)
            }
        }

        return element.apply { elements = newElements }
    }
}
