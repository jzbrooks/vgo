package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path

class RemoveTransparentPaths : TopDownOptimization {
    override fun visit(graphic: Graphic) = removeTransparentPaths(graphic)
    override fun visit(group: Group) = removeTransparentPaths(group)
    override fun visit(clipPath: ClipPath) {}
    override fun visit(extra: Extra) {}
    override fun visit(path: Path) {}

    private fun removeTransparentPaths(containerElement: ContainerElement) {
        // If a path has an id, it might be used in an animation
        // or otherwise referenced elsewhere
        containerElement.elements = containerElement.elements.filter { element ->
            element !is Path || element.id != null ||
                (element.fill.alpha != 0.toUByte() || element.stroke.alpha != 0.toUByte())
        }
    }
}
