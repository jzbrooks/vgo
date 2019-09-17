package com.jzbrooks.guacamole.graphic.command

data class VerticalLineTo(override var variant: CommandVariant, var parameters: List<Float>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'V'
            CommandVariant.RELATIVE -> 'v'
        }

        return "$command${parameters.joinToString(separator = " ") { it.compactString() }}"
    }
}