package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.graphic.command.Command

data class Path(
    override var commands: List<Command>,
    override val id: String? = null,
    override val foreign: MutableMap<String, String> = mutableMapOf(),
) : PathElement {
    override fun hasSameAttributes(other: PathElement): Boolean {
        return other is Path && id == other.id && foreign == other.foreign
    }
}
