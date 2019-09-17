package com.jzbrooks.guacamole.graphic.command

data class HorizontalLineTo(override var variant: CommandVariant, var parameters: List<Float>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'H'
            CommandVariant.RELATIVE -> 'h'
        }

        return "$command${parameters.joinToString(separator = " ") { it.compactString() }}"
    }
}