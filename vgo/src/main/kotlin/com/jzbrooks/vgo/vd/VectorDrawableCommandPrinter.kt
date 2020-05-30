package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.util.math.Point
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

class VectorDrawableCommandPrinter(private val decimalDigits: Int): CommandPrinter {
    private val epsilon = 0.1.pow(decimalDigits.toDouble())
    private val floatPrecisionScaleFactor by lazy {
        10.0.pow(decimalDigits.toDouble())
    }

    override fun print(command: Command): String {
        return when(command) {
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
    }

    private fun print(moveTo: MoveTo): String {
        val command = when (moveTo.variant) {
            CommandVariant.ABSOLUTE -> 'M'
            CommandVariant.RELATIVE -> 'm'
        }

        return buildString {
            append(command)
            append(print(moveTo.parameters.first()))

            for (parameter in moveTo.parameters.drop(1)) {
                if (parameter.x >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(lineTo: LineTo): String {
        val command = when (lineTo.variant) {
            CommandVariant.ABSOLUTE -> 'L'
            CommandVariant.RELATIVE -> 'l'
        }

        return buildString {
            append(command)
            append(print(lineTo.parameters.first()))

            for (parameter in lineTo.parameters.drop(1)) {
                if (parameter.x >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(verticalLineTo: VerticalLineTo): String {
        val command = when (verticalLineTo.variant) {
            CommandVariant.ABSOLUTE -> 'V'
            CommandVariant.RELATIVE -> 'v'
        }

        return buildString {
            append(command)
            append(print(verticalLineTo.parameters.first()))

            for (parameter in verticalLineTo.parameters.drop(1)) {
                if (parameter >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(horizontalLineTo: HorizontalLineTo): String {
        val command = when (horizontalLineTo.variant) {
            CommandVariant.ABSOLUTE -> 'H'
            CommandVariant.RELATIVE -> 'h'
        }

        return buildString {
            append(command)
            append(print(horizontalLineTo.parameters.first()))

            for (parameter in horizontalLineTo.parameters.drop(1)) {
                if (parameter >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(cubicBezierCurve: CubicBezierCurve): String {
        val command = when (cubicBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> 'C'
            CommandVariant.RELATIVE -> 'c'
        }

        return buildString {
            append(command)
            append(print(cubicBezierCurve.parameters.first()))

            for (parameter in cubicBezierCurve.parameters.drop(1)) {
                if (parameter.startControl.x >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(smoothCubicBezierCurve: SmoothCubicBezierCurve): String {
        val command = when (smoothCubicBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> 'S'
            CommandVariant.RELATIVE -> 's'
        }

        return buildString {
            append(command)
            append(print(smoothCubicBezierCurve.parameters.first()))

            for (parameter in smoothCubicBezierCurve.parameters.drop(1)) {
                if (parameter.endControl.x >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(quadraticBezierCurve: QuadraticBezierCurve): String {
        val command = when (quadraticBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> 'Q'
            CommandVariant.RELATIVE -> 'q'
        }

        return buildString {
            append(command)
            append(print(quadraticBezierCurve.parameters.first()))

            for (parameter in quadraticBezierCurve.parameters.drop(1)) {
                if (parameter.control.x >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(smoothQuadraticBezierCurve: SmoothQuadraticBezierCurve): String {
        val command = when (smoothQuadraticBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> 'T'
            CommandVariant.RELATIVE -> 't'
        }

        return buildString {
            append(command)
            append(print(smoothQuadraticBezierCurve.parameters.first()))

            for (parameter in smoothQuadraticBezierCurve.parameters.drop(1)) {
                if (parameter.x >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(ellipticalArcCurve: EllipticalArcCurve): String {
        val command = when (ellipticalArcCurve.variant) {
            CommandVariant.ABSOLUTE -> 'A'
            CommandVariant.RELATIVE -> 'a'
        }

        return buildString {
            append(command)
            append(print(ellipticalArcCurve.parameters.first()))

            for (parameter in ellipticalArcCurve.parameters.drop(1)) {
                if (parameter.radiusX >= 0) {
                    append(' ')
                }

                append(print(parameter))
            }
        }
    }

    private fun print(float: Float): String {
        val rounded = (float * floatPrecisionScaleFactor).roundToInt() / floatPrecisionScaleFactor
        val roundedInt = rounded.toInt()

        return if (abs(rounded.rem(1f)) < epsilon) {
            roundedInt.toString()
        } else {
            rounded.toFloat().toString()
        }
    }

    private fun print(point: Point) = buildString {
        append(print(point.x))
        append(',')
        append(print(point.y))
    }

    private fun print(parameter: CubicBezierCurve.Parameter): String {
        return parameter.run {
            buildString {
                append(print(startControl))
                if (endControl.x >= 0) append(' ')
                append(print(endControl))
                if (end.x >= 0) append(' ')
                append(print(end))
            }
        }
    }

    private fun print(parameter: SmoothCubicBezierCurve.Parameter): String {
        return parameter.run {
            buildString {
                append(print(endControl))
                if (end.x >= 0) append(' ')
                append(print(end))
            }
        }
    }

    private fun print(parameter: QuadraticBezierCurve.Parameter): String {
        return parameter.run {
            buildString {
                append(print(control))
                if (end.x >= 0) append(' ')
                append(print(end))
            }
        }
    }

    private fun print(parameter: EllipticalArcCurve.Parameter): String {
        return parameter.run {
            buildString {
                append(print(radiusX))
                if (radiusY >= 0) append(',')
                append(print(radiusY))
                if (angle >= 0) append(',')
                append(print(angle))
                append(',')
                append(arc.ordinal)
                append(',')
                append(sweep.ordinal)
                append(',')
                append(print(end))
            }
        }
    }
}
