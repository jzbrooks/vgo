package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.graphic.command.Command

data class Path(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    var commands: List<Command>,
    val fill: Color,
    val fillRule: FillRule,
    val stroke: Color,
    val strokeWidth: Float,
    val strokeLineCap: LineCap,
    val strokeLineJoin: LineJoin,
    val strokeMiterLimit: Float,
) : Element {

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
