package com.jzbrooks.avdo.graphic.command

data class MoveTo(override val variant: VariantCommand.Variant, val arguments: List<Pair<Int, Int>>) : VariantCommand