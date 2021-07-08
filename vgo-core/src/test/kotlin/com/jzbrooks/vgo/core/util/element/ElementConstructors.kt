package com.jzbrooks.vgo.core.util.element

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.ElementVisitor
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command

fun createGraphic(
    elements: List<Element> = emptyList(),
    id: String? = null,
    foreign: MutableMap<String, String> = mutableMapOf()
) = object : Graphic {
    override var elements: List<Element> = elements
    override val id: String? = id
    override val foreign: MutableMap<String, String> = foreign

    override fun accept(visitor: ElementVisitor) = visitor.visit(this)
}

fun createPath(
    commands: List<Command> = emptyList(),
    id: String? = null,
    foreign: MutableMap<String, String> = mutableMapOf(),
    fill: Color = Colors.BLACK,
    fillRule: Path.FillRule = Path.FillRule.NON_ZERO,
    stroke: Color = Colors.TRANSPARENT,
    strokeWidth: Float = 1f,
    strokeLineCap: Path.LineCap = Path.LineCap.BUTT,
    strokeLineJoin: Path.LineJoin = Path.LineJoin.MITER,
    strokeMiterLimit: Float = 4f,
) = Path(
    id,
    foreign,
    commands,
    fill,
    fillRule,
    stroke,
    strokeWidth,
    strokeLineCap,
    strokeLineJoin,
    strokeMiterLimit,
)
