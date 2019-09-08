package com.jzbrooks.avdo.graphic.command

data class HorizontalLineTo(override val variant: CommandVariant, val parameters: List<Float>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'H'
            CommandVariant.RELATIVE -> 'h'
        }

        return "$command${parameters.joinToString(separator = " ") { it.compactString() }}"
    }
}