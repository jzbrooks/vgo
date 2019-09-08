package com.jzbrooks.avdo.graphic.command

data class EllipticalArcCurve(override val variant: CommandVariant, val parameters: List<Parameter>) : VariantCommand {
    data class Parameter(val radiusX: Float, val radiusY: Float, val angle: Float, val arc: ArcFlag, val sweep: SweepFlag, val end: Point<Float>)
    enum class ArcFlag {
        LARGE,
        SMALL
    }
    enum class SweepFlag {
        CLOCKWISE,
        ANTICLOCKWISE
    }
}