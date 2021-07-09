package com.jzbrooks.vgo.core.graphic.command

sealed interface ParameterizedCommand<T> : Command {
    var variant: CommandVariant
    var parameters: List<T>
}
