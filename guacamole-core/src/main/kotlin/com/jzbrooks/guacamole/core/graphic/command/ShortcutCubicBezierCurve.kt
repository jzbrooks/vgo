package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point

data class ShortcutCubicBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<ShortcutCubicBezierCurve.Parameter> {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'S'
            CommandVariant.RELATIVE -> 's'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(var endControl: Point, var end: Point) {
        override fun toString() = "$endControl $end"
    }
}