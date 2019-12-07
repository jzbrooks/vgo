package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point

data class LineTo(
        override var variant: CommandVariant,
        override var parameters: List<Point>
) : ParameterizedCommand<Point>