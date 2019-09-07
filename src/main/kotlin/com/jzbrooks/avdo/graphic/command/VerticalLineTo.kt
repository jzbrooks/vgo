package com.jzbrooks.avdo.graphic.command

data class VerticalLineTo(override val variant: CommandVariant, val arguments: List<Float>) : VariantCommand