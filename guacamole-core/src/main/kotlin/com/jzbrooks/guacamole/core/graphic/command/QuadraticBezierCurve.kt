package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point

data class QuadraticBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<QuadraticBezierCurve.Parameter> {
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
