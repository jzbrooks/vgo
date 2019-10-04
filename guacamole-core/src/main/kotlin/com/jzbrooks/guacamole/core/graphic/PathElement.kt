package com.jzbrooks.guacamole.core.graphic

import com.jzbrooks.guacamole.core.graphic.command.Command

interface PathElement : Element {
    var commands: List<Command>
}