package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point
import java.lang.IllegalArgumentException
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

class CommandPrinter(private val decimalDigits: Int) {
    private val epsilon = 0.1.pow(decimalDigits.toDouble())
    private val floatPrecisionScaleFactor by lazy {
        10.0.pow(decimalDigits.toDouble())
    }

    fun print(command: Command): String {
        return when(command) {
            is MoveTo -> print(command)
            is LineTo -> print(command)
            is VerticalLineTo -> print(command)
            is HorizontalLineTo -> print(command)
            is CubicBezierCurve -> print(command)
            is ShortcutCubicBezierCurve -> print(command)
            is QuadraticBezierCurve -> print(command)
            is ShortcutQuadraticBezierCurve -> print(command)
            is EllipticalArcCurve -> print(command)
            is ClosePath -> "Z"
            else -> throw IllegalArgumentException("An unexpected command type was encountered: $command")
        }
    }

    private fun print(moveTo: MoveTo): String {
        val command = when (moveTo.variant) {
            CommandVariant.ABSOLUTE -> 'M'
            CommandVariant.RELATIVE -> 'm'
        }

        val builder = StringBuilder()
        for ((index, parameter) in moveTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != moveTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(lineTo: LineTo): String {
        val command = when (lineTo.variant) {
            CommandVariant.ABSOLUTE -> 'L'
            CommandVariant.RELATIVE -> 'l'
        }

        val builder = StringBuilder()
        for ((index, parameter) in lineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != lineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(verticalLineTo: VerticalLineTo): String {
        val command = when (verticalLineTo.variant) {
            CommandVariant.ABSOLUTE -> 'V'
            CommandVariant.RELATIVE -> 'v'
        }

        val builder = StringBuilder()
        for ((index, parameter) in verticalLineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != verticalLineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(horizontalLineTo: HorizontalLineTo): String {
        val command = when (horizontalLineTo.variant) {
            CommandVariant.ABSOLUTE -> 'H'
            CommandVariant.RELATIVE -> 'h'
        }

        val builder = StringBuilder()
        for ((index, parameter) in horizontalLineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != horizontalLineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(cubicBezierCurve: CubicBezierCurve): String {
        val command = when (cubicBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> 'C'
            CommandVariant.RELATIVE -> 'c'
        }

        val builder = StringBuilder()
        for ((index, parameter) in cubicBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != cubicBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(shortcutCubicBezierCurve: ShortcutCubicBezierCurve): String {
        val command = when (shortcutCubicBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> 'S'
            CommandVariant.RELATIVE -> 's'
        }

        val builder = StringBuilder()
        for ((index, parameter) in shortcutCubicBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != shortcutCubicBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(quadraticBezierCurve: QuadraticBezierCurve): String {
        val command = when (quadraticBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> 'Q'
            CommandVariant.RELATIVE -> 'q'
        }

        val builder = StringBuilder()
        for ((index, parameter) in quadraticBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != quadraticBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(shortcutQuadraticBezierCurve: ShortcutQuadraticBezierCurve): String {
        val command = when (shortcutQuadraticBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> 'T'
            CommandVariant.RELATIVE -> 't'
        }

        val builder = StringBuilder()
        for ((index, parameter) in shortcutQuadraticBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != shortcutQuadraticBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(ellipticalArcCurve: EllipticalArcCurve): String {
        val command = when (ellipticalArcCurve.variant) {
            CommandVariant.ABSOLUTE -> 'A'
            CommandVariant.RELATIVE -> 'a'
        }

        val builder = StringBuilder()
        for ((index, parameter) in ellipticalArcCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != ellipticalArcCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return "$command$builder"
    }

    private fun print(float: Float): String {
        val rounded = (float * floatPrecisionScaleFactor).roundToInt() / floatPrecisionScaleFactor

        return if (abs(rounded.rem(1f)) < epsilon) {
            rounded.toInt().toString()
        } else {
            rounded.toFloat().toString().trimEnd('0')
        }
    }

    private fun print(point: Point) = "${print(point.x)},${print(point.y)}"

    private fun print(parameter: CubicBezierCurve.Parameter): String {
        return parameter.run {
            "${print(startControl)} ${print(endControl)} ${print(end)}"
        }
    }

    private fun print(parameter: ShortcutCubicBezierCurve.Parameter): String {
        return parameter.run {
            "${print(endControl)} ${print(end)}"
        }
    }

    private fun print(parameter: QuadraticBezierCurve.Parameter): String {
        return parameter.run {
            "${print(control)} ${print(end)}"
        }
    }

    private fun print(parameter: EllipticalArcCurve.Parameter): String {
        return parameter.run {
            "${print(radiusX)},${print(radiusY)},${print(angle)},${arc.ordinal},${sweep.ordinal},${print(end)}"
        }
    }
}
