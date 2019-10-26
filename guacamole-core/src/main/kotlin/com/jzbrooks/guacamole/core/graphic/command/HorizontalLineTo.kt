package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.compactString

data class HorizontalLineTo(
        override var variant: CommandVariant,
        override var parameters: List<Float>
) : ParameterizedCommand<Float> {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'H'
            CommandVariant.RELATIVE -> 'h'
        }

        return "$command${parameters.joinToString(separator = " ") { it.compactString() }}"
    }
}