package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point

data class EllipticalArcCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<EllipticalArcCurve.Parameter> {

    data class Parameter(var radiusX: Float, var radiusY: Float, var angle: Float, var arc: ArcFlag, var sweep: SweepFlag, var end: Point)

    enum class ArcFlag {
        SMALL,
        LARGE
    }

    enum class SweepFlag {
        ANTICLOCKWISE,
        CLOCKWISE
    }
}