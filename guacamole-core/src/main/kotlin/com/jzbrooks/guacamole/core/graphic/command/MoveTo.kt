package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point

data class MoveTo(
        override var variant: CommandVariant,
        override var parameters: List<Point>
) : ParameterizedCommand<Point>