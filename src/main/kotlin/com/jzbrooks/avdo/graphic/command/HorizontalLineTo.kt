package com.jzbrooks.avdo.graphic.command

data class HorizontalLineTo(override val variant: CommandVariant, val parameters: List<Float>) : VariantCommand