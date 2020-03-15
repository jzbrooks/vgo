package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.*

class CollapseGroups : Optimization {

    // Groups play a serious role in clip paths for
    // Android Vector Drawables. Perhaps this should
    // eventually be a target-specific optimization
    private val Group.isMergeable: Boolean
        get() {
            // todo(jzb): We should probably be more specific about clip paths here
            val firstClipPath = elements.indexOfFirst { it !is Path }
            val lastPath = elements.indexOfLast { it is Path }
            val hasValidClipPath = firstClipPath != -1 && firstClipPath < lastPath

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
