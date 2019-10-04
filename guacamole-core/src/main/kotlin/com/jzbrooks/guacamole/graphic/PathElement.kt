package com.jzbrooks.guacamole.graphic

import com.jzbrooks.guacamole.graphic.command.Command

interface PathElement : Element {
    var commands: List<Command>
}