package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.util.math.Point
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

class ScalableVectorGraphicCommandPrinter(private val decimalDigits: Int): CommandPrinter {
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
            is ShortcutCubicBezierCurve -> print(command)
            is QuadraticBezierCurve -> print(command)
            is ShortcutQuadraticBezierCurve -> print(command)
            is EllipticalArcCurve -> print(command)
            is ClosePath -> "Z"
            else -> throw IllegalArgumentException("An unexpected command type was encountered: $command")
        }
    }

    private fun print(moveTo: MoveTo): String {
        val builder = StringBuilder(when (moveTo.variant) {
            CommandVariant.ABSOLUTE -> "M"
            CommandVariant.RELATIVE -> "m"
        })

        for ((index, parameter) in moveTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != moveTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(lineTo: LineTo): String {
        val builder = StringBuilder(when (lineTo.variant) {
            CommandVariant.ABSOLUTE -> "L"
            CommandVariant.RELATIVE -> "l"
        })

        for ((index, parameter) in lineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != lineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(verticalLineTo: VerticalLineTo): String {
        val builder = StringBuilder(when (verticalLineTo.variant) {
            CommandVariant.ABSOLUTE -> "V"
            CommandVariant.RELATIVE -> "v"
        })

        for ((index, parameter) in verticalLineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != verticalLineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(horizontalLineTo: HorizontalLineTo): String {
        val builder = StringBuilder(when (horizontalLineTo.variant) {
            CommandVariant.ABSOLUTE -> "H"
            CommandVariant.RELATIVE -> "h"
        })

        for ((index, parameter) in horizontalLineTo.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != horizontalLineTo.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(cubicBezierCurve: CubicBezierCurve): String {
        val builder = StringBuilder(when (cubicBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> "C"
            CommandVariant.RELATIVE -> "c"
        })

        for ((index, parameter) in cubicBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != cubicBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(shortcutCubicBezierCurve: ShortcutCubicBezierCurve): String {
        val builder = StringBuilder(when (shortcutCubicBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> "S"
            CommandVariant.RELATIVE -> "s"
        })

        for ((index, parameter) in shortcutCubicBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != shortcutCubicBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(quadraticBezierCurve: QuadraticBezierCurve): String {
        val builder = StringBuilder(
                when (quadraticBezierCurve.variant) {
                    CommandVariant.ABSOLUTE -> "Q"
                    CommandVariant.RELATIVE -> "q"
                }
        )

        for ((index, parameter) in quadraticBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != quadraticBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(shortcutQuadraticBezierCurve: ShortcutQuadraticBezierCurve): String {
        val builder = StringBuilder(when (shortcutQuadraticBezierCurve.variant) {
            CommandVariant.ABSOLUTE -> "T"
            CommandVariant.RELATIVE -> "t"
        })

        for ((index, parameter) in shortcutQuadraticBezierCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != shortcutQuadraticBezierCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(ellipticalArcCurve: EllipticalArcCurve): String {
        val builder = StringBuilder(when (ellipticalArcCurve.variant) {
            CommandVariant.ABSOLUTE -> "A"
            CommandVariant.RELATIVE -> "a"
        })

        for ((index, parameter) in ellipticalArcCurve.parameters.withIndex()) {
            builder.append(print(parameter))
            if (index != ellipticalArcCurve.parameters.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    private fun print(float: Float): String {
        val rounded = (float * floatPrecisionScaleFactor).roundToInt() / floatPrecisionScaleFactor
        val roundedInt = rounded.toInt()

        return when {
            abs(rounded.rem(1f)) < epsilon -> roundedInt.toString()
            roundedInt == 0 -> {
                if (rounded < 0) {
                    "-${rounded.toFloat().toString().trim('-', '0')}"
                } else {
                    rounded.toFloat().toString().trim('0')
                }
            }
            else -> rounded.toFloat().toString()
        }
    }

    private fun print(point: Point): String {
        val builder = StringBuilder(print(point.x))

        when {
            (point.y < 1 && point.y > 0 && point.x != 0f) -> {
                builder.append(print(point.y).trimStart('0'))
            }
            (point.y > -1 && point.y < 0 && point.x != 0f) -> {
                val y = print(point.y).trimStart('-', '0')
                builder.append('-')
                builder.append(y)
            }
            else -> {
                builder.append(',')
                builder.append(print(point.y))
            }
        }

        return builder.toString()
    }

    private fun print(parameter: CubicBezierCurve.Parameter): String {
        return parameter.run {
            val builder = StringBuilder(print(startControl))
            if (endControl.x >= 0) builder.append(' ')
            builder.append(print(endControl))
            if (end.x >= 0) builder.append(' ')
            builder.append(print(end))
            builder.toString()
        }
    }

    private fun print(parameter: ShortcutCubicBezierCurve.Parameter): String {
        return parameter.run {
            val builder = StringBuilder(print(endControl))
            if (end.x >= 0) builder.append(' ')
            builder.append(print(end))
            builder.toString()
        }
    }

    private fun print(parameter: QuadraticBezierCurve.Parameter): String {
        return parameter.run {
            val builder = StringBuilder(print(control))
            if (end.x >= 0) builder.append(' ')
            builder.append(print(end))
            builder.toString()
        }
    }

    private fun print(parameter: EllipticalArcCurve.Parameter): String {
        return parameter.run {
            val builder = StringBuilder(print(radiusX))
            if (radiusY >= 0) builder.append(',')
            builder.append(print(radiusY))
            if (angle >= 0) builder.append(',')
            builder.append(print(angle))
            builder.append(',')
            builder.append(arc.ordinal)
            builder.append(sweep.ordinal)
            if (end.x >= 0) builder.append(',')
            builder.append(print(end))
            builder.toString()
        }
    }
}
