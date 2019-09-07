package com.jzbrooks.avdo.graphic.command

data class LineTo(override val variant: CommandVariant, val arguments: List<Pair<Float, Float>>) : VariantCommand