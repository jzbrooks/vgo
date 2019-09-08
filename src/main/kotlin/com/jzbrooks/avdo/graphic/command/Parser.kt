package com.jzbrooks.avdo.graphic.command

import java.lang.IllegalStateException

inline class CommandString(val data: String) {
    fun toCommandList(): List<Command> {
        return data.split(commandRegex)
                .asSequence()
                .filter { it.isNotBlank() }
                .map { command ->
                    val upperCommand = command.toUpperCase()
                    val variant = if (command[0].isLowerCase()) CommandVariant.RELATIVE else CommandVariant.ABSOLUTE
                    when {
                        upperCommand.startsWith("M") -> {
                            val data = points.findAll(command)
                                    .map(::mapPoint)
                                    .toList()
                            MoveTo(variant, data)
                        }
                        upperCommand.startsWith("L") -> {
                            val data = points.findAll(command)
                                    .map(::mapPoint)
                                    .toList()

                            LineTo(variant, data)
                        }
                        upperCommand.startsWith("V") -> {
                            val data = number.findAll(command)
                                    .map { it.value.toFloat() }
                                    .toList()

                            VerticalLineTo(variant, data)
                        }
                        upperCommand.startsWith("H") -> {
                            val data = number.findAll(command)
                                    .map { it.value.toFloat() }
                                    .toList()

                            HorizontalLineTo(variant, data)
                        }
                        upperCommand.startsWith('Q') -> {
                            val data = quadraticBezierCurveParameters.findAll(command)
                                    .map(::mapQuadraticBezierCurveParameter)
                                    .toList()

                            QuadraticBezierCurve(variant, data)
                        }
                        upperCommand.startsWith('C') -> {
                            val data = cubicBezierCurveParameters.findAll(command)
                                    .map(::mapCubicBezierCurveParameter)
                                    .toList()

                            CubicBezierCurve(variant, data)
                        }
                        command.startsWith("Z") -> ClosePath()
                        else -> throw IllegalStateException("Expected one of $commandRegex but was $command")
                    }
                }.toList()
    }

    private fun mapPoint(match: MatchResult): Point<Float> {
        val components = match.value.split(separator)
        return Point(components[0].toFloat(), components[1].toFloat())
    }

    private fun mapQuadraticBezierCurveParameter(match: MatchResult): QuadraticBezierCurve.Parameter {
        val components = match.value.split(separator)
        val control = Point(components[0].toFloat(), components[1].toFloat())
        val end = Point(components[2].toFloat(), components[3].toFloat())

        return QuadraticBezierCurve.Parameter(control, end)
    }

    private fun mapCubicBezierCurveParameter(match: MatchResult): CubicBezierCurve.Parameter {
        val components = match.value.split(separator)
        val startControl = Point(components[0].toFloat(), components[1].toFloat())
        val endControl = Point(components[2].toFloat(), components[3].toFloat())
        val end = Point(components[4].toFloat(), components[5].toFloat())

        return CubicBezierCurve.Parameter(startControl, endControl, end)
    }

    companion object {
        private val commandRegex = Regex("(?=[MmLlHhVvCcSsQqTtAaZz])\\s*")
        private val number = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)([eE][-+]?\\d+)?")
        private val separator = Regex("[,\\s]")
        private val points = Regex(Array(2) { number.pattern }.joinToString(separator = separator.pattern))
        private val quadraticBezierCurveParameters = Regex(Array(2) { points.pattern }.joinToString(separator = separator.pattern))
        private val cubicBezierCurveParameters = Regex(Array(3) { points.pattern }.joinToString(separator = separator.pattern))
    }
}
