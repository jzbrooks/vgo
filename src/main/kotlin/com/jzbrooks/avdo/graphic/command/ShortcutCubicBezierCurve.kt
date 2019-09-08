package com.jzbrooks.avdo.graphic.command

data class ShortcutCubicBezierCurve(override val variant: CommandVariant, val parameters: List<Parameter>) : VariantCommand {
    data class Parameter(val endControl: Point<Float>, val end: Point<Float>)
}