package com.jzbrooks.avdo.graphic.command

data class ShortcutCubicBezierCurve(override val variant: CommandVariant, val parameters: List<Parameter>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'S'
            CommandVariant.RELATIVE -> 's'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(val endControl: Point, val end: Point) {
        override fun toString() = "$endControl $end"
    }
}