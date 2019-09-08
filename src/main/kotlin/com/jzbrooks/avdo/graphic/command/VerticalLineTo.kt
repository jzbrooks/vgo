package com.jzbrooks.avdo.graphic.command

data class VerticalLineTo(override val variant: CommandVariant, val parameters: List<Float>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'V'
            CommandVariant.RELATIVE -> 'v'
        }

        return "$command${parameters.joinToString(separator = " ") { it.compactString() }}"
    }
}