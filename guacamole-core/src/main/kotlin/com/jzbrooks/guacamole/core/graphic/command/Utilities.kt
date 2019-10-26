package com.jzbrooks.guacamole.core.graphic.command

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
fun isSameVariant(first: Command, second: Command): Boolean {
    contract {
        returns(true) implies (first is ParameterizedCommand<*>)
        returns(true) implies (second is ParameterizedCommand<*>)
    }

    return first is ParameterizedCommand<*> && second is ParameterizedCommand<*> && first.variant == second.variant
}