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
                        upperCommand.startsWith('M') -> {
                            val parameters = points.findAll(command)
                                    .map(::mapPoint)
                                    .toList()
                            MoveTo(variant, parameters)
                        }
                        upperCommand.startsWith('L') -> {
                            val parameters = points.findAll(command)
                                    .map(::mapPoint)
                                    .toList()

                            LineTo(variant, parameters)
                        }
                        upperCommand.startsWith('V') -> {
                            val parameters = number.findAll(command)
                                    .map { it.value.toFloat() }
                                    .toList()

                            VerticalLineTo(variant, parameters)
                        }
                        upperCommand.startsWith('H') -> {
                            val parameters = number.findAll(command)
                                    .map { it.value.toFloat() }
                                    .toList()

                            HorizontalLineTo(variant, parameters)
                        }
                        upperCommand.startsWith('Q') -> {
                            val parameters = quadraticBezierCurveParameters.findAll(command)
                                    .map(::mapQuadraticBezierCurveParameter)
                                    .toList()

                            QuadraticBezierCurve(variant, parameters)
                        }
                        upperCommand.startsWith('T') -> {
                            val parameters = points.findAll(command)
                                    .map(::mapPoint)
                                    .toList()

                            ShortcutQuadraticBezierCurve(variant, parameters)
                        }
                        upperCommand.startsWith('C') -> {
                            val parameters = cubicBezierCurveParameters.findAll(command)
                                    .map(::mapCubicBezierCurveParameter)
                                    .toList()

                            CubicBezierCurve(variant, parameters)
                        }
                        upperCommand.startsWith('S') -> {
                            val parameters = shortcutCubicBezierCurveParameters.findAll(command)
                                    .map(::mapShortcutCubicBezierCurveParameter)
                                    .toList()

                            ShortcutCubicBezierCurve(variant, parameters)
                        }
                        upperCommand.startsWith('A') -> {
                            val parameters = arcCurveParameters.findAll(command)
                                    .map(::mapEllipticalArcCurveParameter)
                                    .toList()

                            EllipticalArcCurve(variant, parameters)
                        }
                        command.startsWith('Z') -> ClosePath()
                        else -> throw IllegalStateException("Expected one of $commandRegex but was $command")
                    }
                }.toList()
    }

    private fun mapPoint(match: MatchResult): Point {
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

    private fun mapShortcutCubicBezierCurveParameter(match: MatchResult): ShortcutCubicBezierCurve.Parameter {
        val components = match.value.split(separator)
        val endControl = Point(components[0].toFloat(), components[1].toFloat())
        val end = Point(components[2].toFloat(), components[3].toFloat())

        return ShortcutCubicBezierCurve.Parameter(endControl, end)
    }

    private fun mapEllipticalArcCurveParameter(match: MatchResult): EllipticalArcCurve.Parameter {
        val components = match.value.split(separator)
        val radiusX = components[0].toFloat()
        val radiusY = components[1].toFloat()
        val angle = components[2].toFloat()
        val arcFlag = if (components[3].toInt() == 1) EllipticalArcCurve.ArcFlag.LARGE else EllipticalArcCurve.ArcFlag.SMALL
        val sweepFlag = if (components[4].toInt() == 1) EllipticalArcCurve.SweepFlag.CLOCKWISE else EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
        val end = Point(components[5].toFloat(), components[6].toFloat())

        return EllipticalArcCurve.Parameter(radiusX, radiusY, angle, arcFlag, sweepFlag, end)
    }

    companion object {
        private val commandRegex = Regex("(?=[MmLlHhVvCcSsQqTtAaZz])\\s*")
        private val number = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)([eE][-+]?\\d+)?")
        private val separator = Regex("[,\\s]")
        private val points = Regex(Array(2) { number.pattern }.joinToString(separator = separator.pattern))
        private val quadraticBezierCurveParameters = Regex(Array(2) { points.pattern }.joinToString(separator = separator.pattern))
        private val cubicBezierCurveParameters = Regex(Array(3) { points.pattern }.joinToString(separator = separator.pattern))
        private val shortcutCubicBezierCurveParameters = Regex(Array(2) { points.pattern }.joinToString(separator = separator.pattern))
        private val arcCurveParameters = Regex(Array(7) { number.pattern }.joinToString(separator = separator.pattern))
    }
}
