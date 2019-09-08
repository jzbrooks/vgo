package com.jzbrooks.avdo.graphic.command

data class ShortcutQuadraticBezierCurve(override val variant: CommandVariant, val parameters: List<Point>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'T'
            CommandVariant.RELATIVE -> 't'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }
}