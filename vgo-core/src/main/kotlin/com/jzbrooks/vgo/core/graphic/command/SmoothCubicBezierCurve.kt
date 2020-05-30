package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point

data class SmoothCubicBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : CubicCurve<SmoothCubicBezierCurve.Parameter> {
    data class Parameter(var endControl: Point, override var end: Point) : CommandParameter
}