package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point

@JvmInline
value class CommandString(
    val data: String,
) {
    fun toCommandList(): List<Command> =
        data
            .split(commandRegex)
            .asSequence()
            .filter { it.isNotBlank() }
            .map { command ->
                val variant = if (command[0].isLowerCase()) CommandVariant.RELATIVE else CommandVariant.ABSOLUTE
                when {
                    command.startsWith('M', true) -> {
                        val parameters =
                            number
                                .findAll(command)
                                .map(MatchResult::value)
                                .map(String::toFloat)
                                .chunked(2)
                                .map(::mapPoint)
                                .toList()

                        MoveTo(variant, parameters)
                    }
                    command.startsWith('L', true) -> {
                        val parameters =
                            number
                                .findAll(command)
                                .map(MatchResult::value)
                                .map(String::toFloat)
                                .chunked(2)
                                .map(::mapPoint)
                                .toList()

                        LineTo(variant, parameters)
                    }
                    command.startsWith('V', true) -> {
                        val parameters =
                            number
                                .findAll(command)
                                .map { it.value.toFloat() }
                                .toList()

                        VerticalLineTo(variant, parameters)
                    }
                    command.startsWith('H', true) -> {
                        val parameters =
                            number
                                .findAll(command)
                                .map { it.value.toFloat() }
                                .toList()

                        HorizontalLineTo(variant, parameters)
                    }
                    command.startsWith('Q', true) -> {
                        val parameters =
                            number
                                .findAll(command)
                                .map(MatchResult::value)
                                .map(String::toFloat)
                                .chunked(4)
                                .map(::mapQuadraticBezierCurveParameter)
                                .toList()

                        QuadraticBezierCurve(variant, parameters)
                    }
                    command.startsWith('T', true) -> {
                        val parameters =
                            number
                                .findAll(command)
                                .map(MatchResult::value)
                                .map(String::toFloat)
                                .chunked(2)
                                .map(::mapPoint)
                                .toList()

                        SmoothQuadraticBezierCurve(variant, parameters)
                    }
                    command.startsWith('C', true) -> {
                        val parameters =
                            number
                                .findAll(command)
                                .map(MatchResult::value)
                                .map(String::toFloat)
                                .chunked(6)
                                .map(::mapCubicBezierCurveParameter)
                                .toList()

                        CubicBezierCurve(variant, parameters)
                    }
                    command.startsWith('S', true) -> {
                        val parameters =
                            number
                                .findAll(command)
                                .map(MatchResult::value)
                                .map(String::toFloat)
                                .chunked(4)
                                .map(::mapShortcutCubicBezierCurveParameter)
                                .toList()

                        SmoothCubicBezierCurve(variant, parameters)
                    }
                    command.startsWith('A', true) -> {
                        val parameters =
                            buildList {
                                val parameterValues = number.findAll(command).map(MatchResult::value).toMutableList()
                                while (parameterValues.size > 5) {
                                    if (parameterValues[3].length > 1) {
                                        val flagShorthand = parameterValues[3]

                                        add(
                                            EllipticalArcCurve.Parameter(
                                                parameterValues.removeFirst().toFloat(),
                                                parameterValues.removeFirst().toFloat(),
                                                parameterValues.removeFirst().toFloat(),
                                                mapArcFlag(flagShorthand[0].toString().toFloat()).also {
                                                    parameterValues.removeFirst()
                                                },
                                                mapSweepFlag(flagShorthand[1].toString().toFloat()),
                                                Point(
                                                    parameterValues.removeFirst().toFloat(),
                                                    parameterValues.removeFirst().toFloat(),
                                                ),
                                            ),
                                        )
                                    } else {
                                        check(parameterValues.size > 6) {
                                            "$parameterValues must have 7 distinct numerical arguments if the" +
                                                " flags are not combined without an argument separator (space or comma)"
                                        }

                                        add(
                                            EllipticalArcCurve.Parameter(
                                                parameterValues.removeFirst().toFloat(),
                                                parameterValues.removeFirst().toFloat(),
                                                parameterValues.removeFirst().toFloat(),
                                                mapArcFlag(parameterValues.removeFirst().toFloat()),
                                                mapSweepFlag(parameterValues.removeFirst().toFloat()),
                                                Point(
                                                    parameterValues.removeFirst().toFloat(),
                                                    parameterValues.removeFirst().toFloat(),
                                                ),
                                            ),
                                        )
                                    }
                                }
                            }

                        EllipticalArcCurve(variant, parameters)
                    }
                    command.startsWith('Z', true) -> ClosePath
                    else -> throw IllegalStateException("Expected one of $commandRegex but was $command")
                }
            }.toList()

    private fun mapPoint(components: List<Float>): Point = Point(components[0], components[1])

    private fun mapQuadraticBezierCurveParameter(components: List<Float>): QuadraticBezierCurve.Parameter {
        val control = Point(components[0], components[1])
        val end = Point(components[2], components[3])

        return QuadraticBezierCurve.Parameter(control, end)
    }

    private fun mapCubicBezierCurveParameter(components: List<Float>): CubicBezierCurve.Parameter {
        val startControl = Point(components[0], components[1])
        val endControl = Point(components[2], components[3])
        val end = Point(components[4], components[5])

        return CubicBezierCurve.Parameter(startControl, endControl, end)
    }

    private fun mapShortcutCubicBezierCurveParameter(components: List<Float>): SmoothCubicBezierCurve.Parameter {
        val endControl = Point(components[0], components[1])
        val end = Point(components[2], components[3])

        return SmoothCubicBezierCurve.Parameter(endControl, end)
    }

    private fun mapArcFlag(value: Float): EllipticalArcCurve.ArcFlag =
        when (value) {
            1f -> EllipticalArcCurve.ArcFlag.LARGE
            0f -> EllipticalArcCurve.ArcFlag.SMALL
            else -> throw IllegalArgumentException("Unexpected elliptical curve arc flag value: $value\nExpected 0 or 1.")
        }

    private fun mapSweepFlag(value: Float): EllipticalArcCurve.SweepFlag =
        when (value) {
            1f -> EllipticalArcCurve.SweepFlag.CLOCKWISE
            0f -> EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
            else -> throw IllegalArgumentException("Unexpected elliptical curve sweep flag value: $value\nExpected 0 or 1.")
        }

    companion object {
        private val commandRegex = Regex("(?=[MmLlHhVvCcSsQqTtAaZz])\\s*")
        private val number = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?")
    }
}
