package com.jzbrooks.avdo.graphic.command

data class QuadraticBezierCurve(override var variant: CommandVariant, var parameters: List<Parameter>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'Q'
            CommandVariant.RELATIVE -> 'q'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(var control: Point, var end: Point) {
        override fun toString() = "$control $end"
    }
}