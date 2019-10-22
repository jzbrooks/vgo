package com.jzbrooks.guacamole.core.graphic

import com.jzbrooks.guacamole.core.graphic.command.Command

data class Path(
    override var commands: List<Command>,
    override var attributes: MutableMap<String, String> = mutableMapOf()
): PathElement