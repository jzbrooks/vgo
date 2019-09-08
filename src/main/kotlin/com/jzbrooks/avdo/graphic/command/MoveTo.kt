package com.jzbrooks.avdo.graphic.command

data class MoveTo(override val variant: CommandVariant, val parameters: List<Point<Float>>) : VariantCommand