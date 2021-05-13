package com.jzbrooks.vgo.vd.graphic

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.Command

data class ClipPath(
    override var commands: List<Command>,
    override val id: String?,
    override val foreign: MutableMap<String, String>,
) : PathElement {

    override fun hasSameAttributes(other: PathElement): Boolean {
        return id == other.id && foreign == other.foreign
    }
}
