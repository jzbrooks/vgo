package com.jzbrooks.avdo.graphic.command

data class VerticalLineTo(override val variant: VariantCommand.Variant, val arguments: List<Int>) : VariantCommand