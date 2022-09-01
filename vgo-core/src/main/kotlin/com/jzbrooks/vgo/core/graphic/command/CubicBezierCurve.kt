package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point
import dev.romainguy.kotlin.math.Float2

data class CubicBezierCurve(
    override var variant: CommandVariant,
    override var parameters: List<Parameter>
) : CubicCurve<CubicBezierCurve.Parameter> {
    data class Parameter(var startControl: Float2, var endControl: Float2, override var end: Float2) : CommandParameter
}
