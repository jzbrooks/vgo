package com.jzbrooks.guacamole.core.graphic.command

data class VerticalLineTo(
        override var variant: CommandVariant,
        override var parameters: List<Float>
) : ParameterizedCommand<Float>