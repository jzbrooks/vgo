package com.jzbrooks.avdo.graphic.command

data class CubicBezierCurve(override val variant: CommandVariant, val parameters: List<Parameter>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'C'
            CommandVariant.RELATIVE -> 'c'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(val startControl: Point, val endControl: Point, val end: Point) {
        override fun toString() = "$startControl $endControl $end"
    }
}
