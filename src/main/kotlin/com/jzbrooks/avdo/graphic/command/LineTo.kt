package com.jzbrooks.avdo.graphic.command

data class LineTo(override val variant: CommandVariant, val parameters: List<Point<Float>>) : VariantCommand