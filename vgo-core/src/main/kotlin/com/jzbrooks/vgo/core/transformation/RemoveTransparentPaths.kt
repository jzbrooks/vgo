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
    private val inheritedPaint = ArrayDeque<ForeignPaint>()

    override fun visit(graphic: Graphic) {
        inheritedPaint.clear()
        pushPaint(graphic)
        removeTransparentChildren(graphic)
    }

    override fun visit(group: Group) {
        pushPaint(group)
        removeTransparentChildren(group)
    }

    override fun visit(extra: Extra) {
        pushPaint(extra)
    }

    private fun pushPaint(container: ContainerElement) {
        val parentPaint = inheritedPaint.lastOrNull() ?: ForeignPaint.NONE
        inheritedPaint.addLast(inheritance.descend(container.foreign, parentPaint))
    }

    override fun exit(container: ContainerElement) {
        inheritedPaint.removeLast()
        if (container is Graphic) check(inheritedPaint.isEmpty())
    }

    override fun visit(shape: Shape) {}

    override fun visit(path: Path) {}

    private fun removeTransparentChildren(container: ContainerElement) {
        container.elements = container.elements.filter { it.paintsSomething(inheritedPaint.last()) }
    }

    private fun Element.paintsSomething(inherited: ForeignPaint) =
        this !is Path ||
            // A path with an id might be used in an animation or otherwise referenced elsewhere
            id != null ||
            hasVisibleFillPaint(inherited) ||
            hasVisibleStrokePaint(inherited)
}
