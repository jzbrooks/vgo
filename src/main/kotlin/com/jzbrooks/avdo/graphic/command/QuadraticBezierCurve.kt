package com.jzbrooks.avdo.graphic.command

data class QuadraticBezierCurve(override val variant: CommandVariant, val parameters: List<Parameter>) : VariantCommand {
    data class Parameter(val control: Point<Float>, val end: Point<Float>)
}
