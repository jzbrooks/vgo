package com.jzbrooks.avdo.graphic.command

data class CubicBezierCurve(override val variant: CommandVariant, val parameters: List<Parameter>) : VariantCommand {
    data class Parameter(val startControl: Point<Float>, val endControl: Point<Float>, val end: Point<Float>)
}
