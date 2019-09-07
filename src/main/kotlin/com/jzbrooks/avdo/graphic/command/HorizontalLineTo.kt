package com.jzbrooks.avdo.graphic.command

data class HorizontalLineTo(override val variant: CommandVariant, val arguments: List<Float>) : VariantCommand