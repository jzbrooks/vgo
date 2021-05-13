package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.graphic.command.Command

interface PathElement : Element {
    var commands: List<Command>
    fun hasSameAttributes(other: PathElement): Boolean
}
