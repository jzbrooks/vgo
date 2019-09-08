package com.jzbrooks.avdo.graphic

import com.jzbrooks.avdo.graphic.command.Command

data class Path(val commands: List<Command>, val strokeWidth: Int, override val metadata: Map<String, String> = emptyMap()): Element