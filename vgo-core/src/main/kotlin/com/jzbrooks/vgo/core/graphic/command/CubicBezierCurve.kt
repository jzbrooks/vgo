package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point

data class CubicBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : CubicCurve<CubicBezierCurve.Parameter> {
    data class Parameter(var startControl: Point, var endControl: Point, override var end: Point) : EndPoint
}
