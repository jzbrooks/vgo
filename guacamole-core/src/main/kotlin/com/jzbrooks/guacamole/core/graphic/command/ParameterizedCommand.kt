package com.jzbrooks.guacamole.core.graphic.command

interface ParameterizedCommand<T> : Command {
    var variant: CommandVariant
    var parameters: List<T>
}
