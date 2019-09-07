package com.jzbrooks.avdo.graphic.command

data class LineTo(override val variant: VariantCommand.Variant, val arguments: List<Pair<Float, Float>>) : VariantCommand