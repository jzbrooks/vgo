package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point

data class QuadraticBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<QuadraticBezierCurve.Parameter> {
    data class Parameter(var control: Point, override var end: Point) : CommandParameter
}
