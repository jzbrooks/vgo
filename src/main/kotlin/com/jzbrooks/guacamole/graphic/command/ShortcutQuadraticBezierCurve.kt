package com.jzbrooks.guacamole.graphic.command

data class ShortcutQuadraticBezierCurve(override var variant: CommandVariant, var parameters: List<Point>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'T'
            CommandVariant.RELATIVE -> 't'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }
}