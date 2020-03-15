package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point

data class CubicBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<CubicBezierCurve.Parameter> {
    data class Parameter(var startControl: Point, var endControl: Point, var end: Point)
}
