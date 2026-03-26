package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Paint
import com.jzbrooks.vgo.core.graphic.command.Command

data class Path(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    var commands: List<Command>,
    override val fill: Paint,
    override val fillRule: FillRule,
    override val stroke: Paint,
    override val strokeWidth: Float,
    override val strokeLineCap: LineCap,
    override val strokeLineJoin: LineJoin,
    override val strokeMiterLimit: Float,
) : PaintedElement {
    override fun accept(visitor: ElementVisitor) = visitor.visit(this)

    enum class FillRule {
        NON_ZERO,
        EVEN_ODD,
    }

    enum class LineCap {
        BUTT,
        ROUND,
        SQUARE,
    }

    enum class LineJoin {
        MITER,
        ROUND,
        BEVEL,
        ARCS,
        MITER_CLIP,
    }
}
