package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point

data class QuadraticBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<QuadraticBezierCurve.Parameter> {
    data class Parameter(var control: Point, var end: Point)
}
