package com.jzbrooks.avdo.graphic.command

data class HorizontalLineTo(override val variant: VariantCommand.Variant, val arguments: List<Int>) : VariantCommand