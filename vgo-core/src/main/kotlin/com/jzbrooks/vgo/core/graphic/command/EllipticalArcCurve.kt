package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point

data class EllipticalArcCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<EllipticalArcCurve.Parameter> {

    data class Parameter(var radiusX: Float, var radiusY: Float, var angle: Float, var arc: ArcFlag, var sweep: SweepFlag, override var end: Point) : CommandParameter

    enum class ArcFlag {
        SMALL,
        LARGE
    }

    enum class SweepFlag {
        ANTICLOCKWISE,
        CLOCKWISE
    }
}