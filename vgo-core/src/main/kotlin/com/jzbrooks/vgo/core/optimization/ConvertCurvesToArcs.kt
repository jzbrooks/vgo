package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.util.math.*

class ConvertCurvesToArcs(private val printer: CommandPrinter): TopDownOptimization, PathElementVisitor {

    override fun visit(pathElement: PathElement) {
        val multiCurvePass = collapseMultipleCurves(pathElement.commands)
        val newCommands = convertSingleArcs(multiCurvePass)
        pathElement.commands = newCommands
    }

    private fun collapseMultipleCurves(commands: List<Command>): List<Command> {
        val newCommands = mutableListOf<Command>()
        var replacedCommands = 0

        for (i in commands.indices) {
            if (replacedCommands != 0) {
                replacedCommands -= 1
                continue
            }

            val previousCommand = commands.getOrNull(i - 1)
            val originalCommand = commands[i]
            val currentCommand = originalCommand.let { command ->
                if (command is ShortcutCubicBezierCurve && previousCommand is CubicCurve<*>) {
                    command.toCubicBezierCurve(previousCommand)
                } else {
                    command
                }
            }

            if (currentCommand is CubicBezierCurve) {
                assert(currentCommand.parameters.size == 1)

                val currentParameter = currentCommand.parameters[0]
                val circle = currentCommand.fitCircle()
                if (circle != null && currentCommand.isConvex()) {
                    val radius = circle.radius
                    val sweep = if (currentParameter.end.y * currentParameter.startControl.x -
                            currentParameter.end.x * currentParameter.startControl.y > 0) {
                        EllipticalArcCurve.SweepFlag.CLOCKWISE
                    } else {
                        EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
                    }
                    val arc = EllipticalArcCurve(currentCommand.variant, listOf(
                            EllipticalArcCurve.Parameter(
                                    radius,
                                    radius,
                                    0f,
                                    EllipticalArcCurve.ArcFlag.SMALL,
                                    sweep,
                                    currentParameter.end
                            )
                    ))

                    val pendingCurves = mutableListOf(originalCommand as CubicCurve<*>)
                    val alternativeOutput = mutableListOf<Command>(arc)

                    val relativeCircle = circle.copy(center = circle.center - currentCommand.parameters[0].end)
                    var angle = currentCommand.findArcAngle(circle)

                    var j = i + 1
                    var nextCommand = commands.getOrNull(j)
                    val previous = computeAbsoluteCoordinates(newCommands)

                    while (nextCommand is CubicCurve<*> && nextCommand.isConvex() && nextCommand.liesOnCircle(relativeCircle)) {
                        val originalNext = nextCommand
                        if (nextCommand is ShortcutCubicBezierCurve) {
                            nextCommand = nextCommand.toCubicBezierCurve(currentCommand as CubicCurve<*>)
                        }

                        check(nextCommand is CubicBezierCurve)

                        angle += nextCommand.findArcAngle(relativeCircle)

                        if (angle - 2 * Math.PI > 1e-3) {
                            replacedCommands += 1
                            break
                        }

                        if (angle > Math.PI) arc.parameters[0].arc = EllipticalArcCurve.ArcFlag.LARGE

                        pendingCurves.add(originalNext)

                        val next = computeAbsoluteCoordinates(commands, j + 1)

                        if (2 * Math.PI - angle > 1e-3) {
                            arc.parameters[0].end = next - previous
                        } else {
                            arc.parameters[0].end = (relativeCircle.center - nextCommand.parameters[0].end) * 2f
                            alternativeOutput.add(EllipticalArcCurve(CommandVariant.RELATIVE, listOf(
                                    EllipticalArcCurve.Parameter(
                                            radius,
                                            radius,
                                            0f,
                                            EllipticalArcCurve.ArcFlag.SMALL,
                                            sweep,
                                            next - (previous + arc.parameters[0].end)
                                    )
                            )))
                        }

                        relativeCircle.center -= nextCommand.parameters[0].end

                        nextCommand = commands.getOrNull(++j)
                    }

                    // Skip curves that have already been considered on the next iteration
                    replacedCommands += pendingCurves.size - 1

                    // If the next curve is a shorthand, it must be converted
                    // to longhand if it the previous curve is replaced with an
                    // elliptical arc.
                    if (nextCommand is ShortcutCubicBezierCurve) {
                        alternativeOutput.add(nextCommand.toCubicBezierCurve(pendingCurves.last()))
                    }

                    if (pendingCurves.joinToString(separator = "", transform = printer::print).length > alternativeOutput.joinToString(separator = "", transform = printer::print).length) {
                        newCommands.addAll(alternativeOutput)
                        if (alternativeOutput.last() is CubicBezierCurve) replacedCommands += 1
                    } else {
                        newCommands.addAll(pendingCurves)
                    }
                } else {
                    newCommands.add(originalCommand)
                }
            } else {
                newCommands.add(originalCommand)
            }
        }

        return newCommands
    }

    private fun convertSingleArcs(commands: List<Command>): List<Command> {
        val newCommands = mutableListOf<Command>()

        for (command in commands) {
            if (command is CubicBezierCurve) {
                assert(command.parameters.size == 1)

                val currentParameter = command.parameters[0]
                val circle = command.fitCircle(1e-2f)
                if (circle != null && command.isConvex()) {
                    val radius = circle.radius
                    val sweep = if (currentParameter.end.y * currentParameter.startControl.x -
                            currentParameter.end.x * currentParameter.startControl.y > 0) {
                        EllipticalArcCurve.SweepFlag.CLOCKWISE
                    } else {
                        EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
                    }
                    val arc = EllipticalArcCurve(command.variant, listOf(
                            EllipticalArcCurve.Parameter(
                                    radius,
                                    radius,
                                    0f,
                                    EllipticalArcCurve.ArcFlag.SMALL,
                                    sweep,
                                    currentParameter.end
                            )
                    ))

                    if (printer.print(arc).length < printer.print(command).length) {
                        newCommands.add(arc)
                    } else {
                        newCommands.add(command)
                    }
                } else {
                    newCommands.add(command)
                }
            } else {
                newCommands.add(command)
            }
        }

        return newCommands
    }
}