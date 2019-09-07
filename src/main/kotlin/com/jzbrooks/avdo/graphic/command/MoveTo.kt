package com.jzbrooks.avdo.graphic.command

data class MoveTo(override val variant: CommandVariant, val arguments: List<Point<Float>>) : VariantCommand