package com.jzbrooks.avdo.graphic.command

data class EllipticalArcCurve(override val variant: CommandVariant, val parameters: List<Parameter>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'A'
            CommandVariant.RELATIVE -> 'a'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(val radiusX: Float, val radiusY: Float, val angle: Float, val arc: ArcFlag, val sweep: SweepFlag, val end: Point) {
        override fun toString() = "${radiusX.compactString()},${radiusY.compactString()},${angle.compactString()},$arc,$sweep,$end"
    }

    enum class ArcFlag {
        LARGE {
            override fun toString() = "1"
        },
        SMALL {
            override fun toString() = "0"
        }
    }

    enum class SweepFlag {
        CLOCKWISE {
            override fun toString() = "1"
        },
        ANTICLOCKWISE {
            override fun toString() = "0"
        }
    }
}