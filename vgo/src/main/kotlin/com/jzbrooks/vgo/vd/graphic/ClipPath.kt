package com.jzbrooks.vgo.vd.graphic

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.Attributes as CoreAttributes

data class ClipPath(
    override var commands: List<Command>,
    override var attributes: Attributes = Attributes(null, mutableMapOf())
) : PathElement {

    data class Attributes(override val name: String?, override val foreign: MutableMap<String, String>) : CoreAttributes
}
