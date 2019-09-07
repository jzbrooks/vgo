package com.jzbrooks.avdo.graphic.command

data class LineTo(override val variant: CommandVariant, val arguments: List<Point<Float>>) : VariantCommand