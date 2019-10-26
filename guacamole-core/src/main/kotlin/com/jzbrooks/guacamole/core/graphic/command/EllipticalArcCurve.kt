package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point
import com.jzbrooks.guacamole.core.util.math.compactString

data class EllipticalArcCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<EllipticalArcCurve.Parameter> {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'A'
            CommandVariant.RELATIVE -> 'a'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(var radiusX: Float, var radiusY: Float, var angle: Float, var arc: ArcFlag, var sweep: SweepFlag, var end: Point) {
        override fun toString() = "${radiusX.compactString()},${radiusY.compactString()},${angle.compactString()},${arc.ordinal},${sweep.ordinal},$end"
    }

    enum class ArcFlag {
        SMALL,
        LARGE
    }

    enum class SweepFlag {
        ANTICLOCKWISE,
        CLOCKWISE
    }
}