package com.jzbrooks.vgo.core.graphic.command

import dev.romainguy.kotlin.math.Float2

data class QuadraticBezierCurve(
    override var variant: CommandVariant,
    override var parameters: List<Parameter>,
) : ParameterizedCommand<QuadraticBezierCurve.Parameter> {
    data class Parameter(var control: Float2, override var end: Float2) : CommandParameter
}
