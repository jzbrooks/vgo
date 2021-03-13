package com.jzbrooks.vgo.vd.graphic

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.Command

data class ClipPath(
    override var commands: List<Command>,
    override var attributes: MutableMap<String, String> = mutableMapOf()
) : PathElement