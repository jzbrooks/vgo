package com.jzbrooks.guacamole.graphic.command

import com.jzbrooks.guacamole.util.math.Point

data class CubicBezierCurve(override var variant: CommandVariant, var parameters: List<Parameter>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'C'
            CommandVariant.RELATIVE -> 'c'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(var startControl: Point, var endControl: Point, var end: Point) {
        override fun toString() = "$startControl $endControl $end"
    }
}
