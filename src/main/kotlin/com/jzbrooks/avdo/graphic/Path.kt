package com.jzbrooks.avdo.graphic

import com.jzbrooks.avdo.graphic.command.Command

data class Path(override var commands: List<Command>, override var metadata: Map<String, String> = emptyMap()): PathElement