package com.jzbrooks.avdo.graphic

import com.jzbrooks.avdo.graphic.command.Command

interface PathElement : Element {
    var commands: List<Command>
}