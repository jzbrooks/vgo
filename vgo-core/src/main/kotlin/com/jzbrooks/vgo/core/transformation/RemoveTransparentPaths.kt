package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.ForeignPaint
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PaintInheritance
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.graphic.hasVisibleFillPaint
import com.jzbrooks.vgo.core.graphic.hasVisibleStrokePaint

/**
 * Removes paths that can't produce pixels because neither their fill nor their stroke
 * paints anything.
 *
 * Whether a path's paint is dead depends on its ancestors in formats that inherit paint,
 * so [inheritance] supplies that rule.
 */
class RemoveTransparentPaths(
    private val inheritance: PaintInheritance = PaintInheritance.NONE,
) : TopDownTransformer {
    // The whole tree is walked from the root because paint no typed field describes
    // accumulates as it descends, and the shared traversal can't carry that context.
    override fun visit(graphic: Graphic) = walk(graphic, ForeignPaint.NONE)

    override fun visit(group: Group) {}

    override fun visit(extra: Extra) {}

    override fun visit(shape: Shape) {}

    override fun visit(path: Path) {}

    private fun walk(
        container: ContainerElement,
        inherited: ForeignPaint,
    ) {
        val childInherited = inheritance.descend(container.foreign, inherited)

        // Extra is passthrough — its own children are left exactly as they were read —
        // but containers nested within it still drop paths that paint nothing.
        if (container !is Extra) {
            container.elements = container.elements.filter { it.paintsSomething(childInherited) }
        }

        for (child in container.elements) {
            if (child is ContainerElement) walk(child, childInherited)
        }
    }

    private fun Element.paintsSomething(inherited: ForeignPaint) =
        this !is Path ||
            // A path with an id might be used in an animation or otherwise referenced elsewhere
            id != null ||
            hasVisibleFillPaint(inherited) ||
            hasVisibleStrokePaint(inherited)
}
