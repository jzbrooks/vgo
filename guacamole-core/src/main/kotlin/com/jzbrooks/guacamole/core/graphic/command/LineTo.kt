package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point

data class LineTo(
        override var variant: CommandVariant,
        override var parameters: List<Point>
) : ParameterizedCommand<Point> {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'L'
            CommandVariant.RELATIVE -> 'l'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }
}