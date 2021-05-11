package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.Attributes as CoreAttributes

data class Path(
    override var commands: List<Command>,
    override var attributes: Attributes = Attributes(null, mutableMapOf()) // todo: maybe remove the default?
) : PathElement {

    data class Attributes(override val id: String?, override val foreign: MutableMap<String, String>) : CoreAttributes
}
