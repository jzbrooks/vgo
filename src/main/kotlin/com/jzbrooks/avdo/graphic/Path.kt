package com.jzbrooks.avdo.graphic

import com.jzbrooks.avdo.graphic.command.Command

data class Path(
        override var commands: List<Command>,
        var strokeWidth: Int,
        override var metadata: Map<String, String> = emptyMap()
): PathElement