package com.jzbrooks.vgo.util.element

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command

fun createPath(
    commands: List<Command> = emptyList(),
    id: String? = null,
    foreign: MutableMap<String, String> = mutableMapOf(),
    fill: Color = Colors.BLACK,
    stroke: Color = Colors.TRANSPARENT,
    strokeWidth: Float = 1f,
) = Path(
    commands,
    id,
    foreign,
    fill,
    stroke,
    strokeWidth,
)
