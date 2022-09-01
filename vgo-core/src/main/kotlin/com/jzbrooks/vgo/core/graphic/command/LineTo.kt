package com.jzbrooks.vgo.core.graphic.command

import dev.romainguy.kotlin.math.Float2

data class LineTo(
    override var variant: CommandVariant,
    override var parameters: List<Float2>,
) : ParameterizedCommand<Float2>
