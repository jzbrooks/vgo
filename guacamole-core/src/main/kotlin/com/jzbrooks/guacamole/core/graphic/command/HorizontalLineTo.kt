package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.compactString

data class HorizontalLineTo(override var variant: CommandVariant, var parameters: List<Float>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'H'
            CommandVariant.RELATIVE -> 'h'
        }

        return "$command${parameters.joinToString(separator = " ") { it.compactString() }}"
    }
}