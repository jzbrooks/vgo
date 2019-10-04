package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point
import com.jzbrooks.guacamole.core.util.math.compactString

data class EllipticalArcCurve(override var variant: CommandVariant, var parameters: List<Parameter>) : VariantCommand {
    override fun toString(): String {
        val command = when (variant) {
            CommandVariant.ABSOLUTE -> 'A'
            CommandVariant.RELATIVE -> 'a'
        }

        return "$command${parameters.joinToString(separator = " ")}"
    }

    data class Parameter(var radiusX: Float, var radiusY: Float, var angle: Float, var arc: ArcFlag, var sweep: SweepFlag, var end: Point) {
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