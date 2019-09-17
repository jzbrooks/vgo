package com.jzbrooks.guacamole.graphic

import com.jzbrooks.guacamole.graphic.command.Command

data class Path(override var commands: List<Command>, override var metadata: Map<String, String> = emptyMap()): PathElement