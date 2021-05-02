package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.graphic.command.Command

data class Path(
    override var commands: List<Command>,
    override var attributes: MutableMap<String, String> = mutableMapOf()
) : PathElement
