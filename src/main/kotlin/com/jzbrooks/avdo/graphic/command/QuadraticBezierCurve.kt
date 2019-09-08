package com.jzbrooks.avdo.graphic.command

data class QuadraticBezierCurve(override val variant: CommandVariant, val parameters: List<Parameter>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'Q'
            CommandVariant.RELATIVE -> 'q'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(val control: Point, val end: Point) {
        override fun toString() = "$control $end"
    }
}
