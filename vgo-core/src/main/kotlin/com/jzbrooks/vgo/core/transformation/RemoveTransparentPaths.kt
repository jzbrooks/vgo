package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Paint
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Shape

class RemoveTransparentPaths : TopDownTransformer {
    override fun visit(graphic: Graphic) = removeTransparentPaths(graphic)

    override fun visit(group: Group) = removeTransparentPaths(group)

    override fun visit(clipPath: ClipPath) {}

    override fun visit(extra: Extra) {}

    override fun visit(shape: Shape) {}

    override fun visit(path: Path) {}

    private fun removeTransparentPaths(containerElement: ContainerElement) {
        containerElement.elements =
            containerElement.elements.filter { element ->
                element !is Path ||
                    // If a path has an id, it might be used in an animation or otherwise referenced elsewhere
                    element.id != null ||
                    // Colors that aren't able to be parsed may remain in the foreign map
                    element.foreign.keys.any { it.contains("color", ignoreCase = true) } ||
                    // If a path isn't transparent, allow it. Gradient paints are never
                    // considered transparent here — we don't introspect their stops.
                    !element.fill.isTransparentColor() ||
                    !element.stroke.isTransparentColor()
            }
    }

    private fun Paint.isTransparentColor(): Boolean = this is Color && alpha == 0.toUByte()
}
