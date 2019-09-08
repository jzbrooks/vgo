package com.jzbrooks.avdo.graphic.command

data class VerticalLineTo(override val variant: CommandVariant, val parameters: List<Float>) : VariantCommand