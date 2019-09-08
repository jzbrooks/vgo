package com.jzbrooks.avdo.graphic.command

data class ShortcutQuadraticBezierCurve(override val variant: CommandVariant, val parameters: List<Point<Float>>) : VariantCommand