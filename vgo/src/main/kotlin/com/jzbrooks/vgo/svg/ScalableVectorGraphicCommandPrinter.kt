package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandPrinter
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.math.Point
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.sign

class ScalableVectorGraphicCommandPrinter(
    private val decimalDigits: Int,
) : CommandPrinter {
    private val formatter =
        DecimalFormat().apply {
            maximumFractionDigits = decimalDigits
            isDecimalSeparatorAlwaysShown = false
            isGroupingUsed = false
            roundingMode = RoundingMode.HALF_UP
            minimumIntegerDigits = 0
            decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
        }

    override fun print(command: Command): String =
        when (command) {
            is MoveTo -> print(command)
            is LineTo -> print(command)
            is VerticalLineTo -> print(command)
            is HorizontalLineTo -> print(command)
            is CubicBezierCurve -> print(command)
            is SmoothCubicBezierCurve -> print(command)
            is QuadraticBezierCurve -> print(command)
            is SmoothQuadraticBezierCurve -> print(command)
            is EllipticalArcCurve -> print(command)
            is ClosePath -> "Z"
            else -> throw IllegalArgumentException("An unexpected command type was encountered: $command")
        }

    private fun print(moveTo: MoveTo): String {
        val builder =
            StringBuilder(
                when (moveTo.variant) {
                    CommandVariant.ABSOLUTE -> "M"
                    CommandVariant.RELATIVE -> "m"
                },
            )

        for ((index, parameter) in moveTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != moveTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(lineTo: LineTo): String {
        val builder =
            StringBuilder(
                when (lineTo.variant) {
                    CommandVariant.ABSOLUTE -> "L"
                    CommandVariant.RELATIVE -> "l"
                },
            )

        for ((index, parameter) in lineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != lineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(verticalLineTo: VerticalLineTo): String {
        val builder =
            StringBuilder(
                when (verticalLineTo.variant) {
                    CommandVariant.ABSOLUTE -> "V"
                    CommandVariant.RELATIVE -> "v"
                },
            )

        for ((index, parameter) in verticalLineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != verticalLineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(horizontalLineTo: HorizontalLineTo): String {
        val builder =
            StringBuilder(
                when (horizontalLineTo.variant) {
                    CommandVariant.ABSOLUTE -> "H"
                    CommandVariant.RELATIVE -> "h"
                },
            )

        for ((index, parameter) in horizontalLineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != horizontalLineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(cubicBezierCurve: CubicBezierCurve): String {
        val builder =
            StringBuilder(
                when (cubicBezierCurve.variant) {
                    CommandVariant.ABSOLUTE -> "C"
                    CommandVariant.RELATIVE -> "c"
                },
            )

        for ((index, parameter) in cubicBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != cubicBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(smoothCubicBezierCurve: SmoothCubicBezierCurve): String {
        val builder =
            StringBuilder(
                when (smoothCubicBezierCurve.variant) {
                    CommandVariant.ABSOLUTE -> "S"
                    CommandVariant.RELATIVE -> "s"
                },
            )

        for ((index, parameter) in smoothCubicBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != smoothCubicBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(quadraticBezierCurve: QuadraticBezierCurve): String {
        val builder =
            StringBuilder(
                when (quadraticBezierCurve.variant) {
                    CommandVariant.ABSOLUTE -> "Q"
                    CommandVariant.RELATIVE -> "q"
                },
            )

        for ((index, parameter) in quadraticBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != quadraticBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(smoothQuadraticBezierCurve: SmoothQuadraticBezierCurve): String {
        val builder =
            StringBuilder(
                when (smoothQuadraticBezierCurve.variant) {
                    CommandVariant.ABSOLUTE -> "T"
                    CommandVariant.RELATIVE -> "t"
                },
            )

        for ((index, parameter) in smoothQuadraticBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != smoothQuadraticBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(ellipticalArcCurve: EllipticalArcCurve): String {
        val builder =
            StringBuilder(
                when (ellipticalArcCurve.variant) {
                    CommandVariant.ABSOLUTE -> "A"
                    CommandVariant.RELATIVE -> "a"
                },
            )

        for ((index, parameter) in ellipticalArcCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != ellipticalArcCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(float: Float) = formatter.format(float)

    private fun print(point: Point) =
        buildString {
            append(print(point.x))
            if (point.y.sign >= 0f) append(',')
            append(print(point.y))
        }

    private fun print(parameter: CubicBezierCurve.Parameter): String =
        parameter.run {
            buildString {
                append(print(startControl))
                if (endControl.x.sign >= 0) append(' ')
                append(print(endControl))
                if (end.x.sign >= 0) append(' ')
                append(print(end))
            }
        }

    private fun print(parameter: SmoothCubicBezierCurve.Parameter): String =
        parameter.run {
            buildString {
                append(print(endControl))
                if (end.x.sign >= 0) append(' ')
                append(print(end))
            }
        }

    private fun print(parameter: QuadraticBezierCurve.Parameter): String =
        parameter.run {
            buildString {
                append(print(control))
                if (end.x.sign >= 0) append(' ')
                append(print(end))
            }
        }

    private fun print(parameter: EllipticalArcCurve.Parameter): String =
        parameter.run {
            buildString {
                append(print(radiusX))
                if (radiusY.sign >= 0) append(',')
                append(print(radiusY))
                if (angle.sign >= 0) append(',')
                append(print(angle))
                append(',')
                append(arc.ordinal)
                append(sweep.ordinal)
                if (end.x.sign >= 0) append(',')
                append(print(end))
            }
        }
}
