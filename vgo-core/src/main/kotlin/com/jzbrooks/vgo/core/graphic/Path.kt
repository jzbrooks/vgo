package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.graphic.command.Command

data class Path(
    override var commands: List<Command>,
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val fill: Color,
    val stroke: Color,
    val strokeWidth: UInt,
) : PathElement {
    override fun hasSameAttributes(other: PathElement): Boolean {
        return other is Path &&
            id == other.id &&
            foreign == other.foreign &&
            fill == other.fill &&
            stroke == other.stroke &&
            strokeWidth == other.strokeWidth
    }
}
