package com.jzbrooks.vgo.core.graphic.command

import dev.romainguy.kotlin.math.Float2

data class SmoothCubicBezierCurve(
    override var variant: CommandVariant,
    override var parameters: List<Parameter>
) : CubicCurve<SmoothCubicBezierCurve.Parameter> {
    data class Parameter(var endControl: Float2, override var end: Float2) : CommandParameter
}
