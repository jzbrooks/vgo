package com.jzbrooks.vgo.util.element

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command

fun createPath(
    commands: List<Command> = emptyList(),
    id: String? = null,
    foreign: MutableMap<String, String> = mutableMapOf(),
    fill: Brush = Colors.BLACK,
    fillRule: Path.FillRule = Path.FillRule.NON_ZERO,
    stroke: Brush = Colors.TRANSPARENT,
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
