package com.jzbrooks.avdo.graphic.command

data class MoveTo(override val variant: CommandVariant, val parameters: List<Point>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'M'
            CommandVariant.RELATIVE -> 'm'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }
}