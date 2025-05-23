package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandPrinter
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.CubicCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.util.math.Circle
import com.jzbrooks.vgo.core.util.math.computeAbsoluteCoordinates
import com.jzbrooks.vgo.core.util.math.findArcAngle
import com.jzbrooks.vgo.core.util.math.fitCircle
import com.jzbrooks.vgo.core.util.math.isConvex
import com.jzbrooks.vgo.core.util.math.liesOnCircle
import com.jzbrooks.vgo.core.util.math.toCubicBezierCurve

/**
 * Converts cubic Bézier curves to arcs when they are shorter.
 */
class ConvertCurvesToArcs(
    private val criterion: Criterion,
) : TopDownTransformer {
    @Deprecated("Use the constructor with criteria instead.")
    constructor(printer: CommandPrinter) : this(Criterion.ShortestPath(printer))

    override fun visit(graphic: Graphic) {}

    override fun visit(clipPath: ClipPath) {}

    override fun visit(group: Group) {}

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {
        val multiCurvePass = collapseMultipleCurves(path.commands)
        if (criterion is Criterion.ShortestPath) {
            val singleCurvePass = criterion.convertSingleArcs(multiCurvePass)
            path.commands = singleCurvePass
        } else {
            path.commands = multiCurvePass
        }
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
            val currentCommand =
                originalCommand.let { command ->
                    if (command is SmoothCubicBezierCurve && previousCommand is CubicCurve<*>) {
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
                    val sweep =
                        if (currentParameter.end.y * currentParameter.startControl.x -
                            currentParameter.end.x * currentParameter.startControl.y > 0
                        ) {
                            EllipticalArcCurve.SweepFlag.CLOCKWISE
                        } else {
                            EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
                        }
                    val arc =
                        EllipticalArcCurve(
                            currentCommand.variant,
                            listOf(
                                EllipticalArcCurve.Parameter(
                                    radius,
                                    radius,
                                    0f,
                                    EllipticalArcCurve.ArcFlag.SMALL,
                                    sweep,
                                    currentParameter.end,
                                ),
                            ),
                        )

                    val pendingCurves = mutableListOf(originalCommand as CubicCurve<*>)
                    val ellipticalArcs = mutableListOf<Command>(arc)

                    val relativeCircle = circle.copy(center = circle.center - currentCommand.parameters[0].end)
                    var angle = currentCommand.findArcAngle(circle)

                    var j = i + 1
                    var nextCommand = commands.getOrNull(j)

                    val previous = computeAbsoluteCoordinates(commands.take(i))

                    if (nextCommand != null) {
                        var currentCurve = currentCommand
                        var nextCurve = convertToCircularCubicCurve(nextCommand, relativeCircle, currentCurve)

                        while (nextCommand is CubicCurve<*> && nextCurve != null) {
                            pendingCurves.add(nextCommand)

                            val next = computeAbsoluteCoordinates(commands.take(j + 1))

                            angle += nextCurve.findArcAngle(relativeCircle)

                            if (angle - 2 * Math.PI > 1e-3) {
                                break
                            }

                            if (angle > Math.PI) arc.parameters[0].arc = EllipticalArcCurve.ArcFlag.LARGE

                            if (2 * Math.PI - angle > 1e-3) {
                                arc.parameters[0].end = next - previous
                            } else {
                                arc.parameters[0].end = (relativeCircle.center - nextCurve.parameters[0].end) * 2f
                                ellipticalArcs.add(
                                    EllipticalArcCurve(
                                        CommandVariant.RELATIVE,
                                        listOf(
                                            EllipticalArcCurve.Parameter(
                                                radius,
                                                radius,
                                                0f,
                                                EllipticalArcCurve.ArcFlag.SMALL,
                                                sweep,
                                                next - (previous + arc.parameters[0].end),
                                            ),
                                        ),
                                    ),
                                )
                                break
                            }

                            relativeCircle.center += nextCurve.parameters[0].end
                            nextCommand = commands.getOrNull(++j)
                            currentCurve = nextCurve
                            nextCurve =
                                nextCommand?.let {
                                    convertToCircularCubicCurve(it, relativeCircle, currentCurve)
                                }
                        }
                    }

                    // If the next curve is shorthand, it must be converted
                    // to longhand if it is, the previous curve is replaced with an
                    // elliptical arc.
                    if (nextCommand is SmoothCubicBezierCurve) {
                        ellipticalArcs.add(nextCommand.toCubicBezierCurve(pendingCurves.last()))
                        pendingCurves.add(nextCommand)
                    }

                    val (originalSize, alternativeSize) =
                        when (criterion) {
                            is Criterion.ShortestPath -> {
                                val originalSize =
                                    pendingCurves
                                        .joinToString(
                                            separator = "",
                                            transform = criterion.printer::print,
                                        ).length
                                val alternativeSize =
                                    ellipticalArcs
                                        .joinToString(
                                            separator = "",
                                            transform = criterion.printer::print,
                                        ).length
                                originalSize to alternativeSize
                            }
                            is Criterion.FewestCommands -> {
                                val originalSize = pendingCurves.size
                                val alternativeSize = ellipticalArcs.size
                                originalSize to alternativeSize
                            }
                        }

                    if (alternativeSize < originalSize) {
                        newCommands.addAll(ellipticalArcs)

                        // Skip curves that have already been considered on the next iteration
                        replacedCommands = j - i - 1

                        if (ellipticalArcs.last() is CubicBezierCurve) replacedCommands += 1
                    } else {
                        newCommands.addAll(pendingCurves)
                        replacedCommands = pendingCurves.size - 1
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

    private fun Criterion.ShortestPath.convertSingleArcs(commands: List<Command>): List<Command> {
        val newCommands = mutableListOf<Command>()
        var fixedUpCurve: CubicBezierCurve? = null

        for (i in commands.indices) {
            val command = fixedUpCurve ?: commands[i]
            fixedUpCurve = null

            if (command is CubicBezierCurve) {
                assert(command.parameters.size == 1)

                val currentParameter = command.parameters[0]
                val circle = command.fitCircle()
                if (circle != null && command.isConvex()) {
                    val radius = circle.radius
                    val sweep =
                        if (currentParameter.end.y * currentParameter.startControl.x -
                            currentParameter.end.x * currentParameter.startControl.y > 0
                        ) {
                            EllipticalArcCurve.SweepFlag.CLOCKWISE
                        } else {
                            EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
                        }
                    val arc =
                        EllipticalArcCurve(
                            command.variant,
                            listOf(
                                EllipticalArcCurve.Parameter(
                                    radius,
                                    radius,
                                    0f,
                                    EllipticalArcCurve.ArcFlag.SMALL,
                                    sweep,
                                    currentParameter.end,
                                ),
                            ),
                        )

                    val next = commands.getOrNull(i + 1)
                    val arcOutput =
                        if (next is SmoothCubicBezierCurve) {
                            listOf(arc, next.toCubicBezierCurve(command))
                        } else {
                            listOf(arc)
                        }

                    val originalSize = printer.print(command).length
                    val alternativeSize = arcOutput.joinToString(separator = "", transform = printer::print).length
                    if (alternativeSize < originalSize) {
                        newCommands.addAll(arcOutput)
                        fixedUpCurve = arcOutput.last() as? CubicBezierCurve
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

    private fun convertToCircularCubicCurve(
        command: Command,
        relativeCircle: Circle,
        previousCurve: CubicBezierCurve?,
    ): CubicBezierCurve? =
        when (command) {
            is CubicBezierCurve -> command
            is SmoothCubicBezierCurve -> command.toCubicBezierCurve(previousCurve!!)
            else -> null
        }?.takeIf { it.isConvex() && it.liesOnCircle(relativeCircle) }

    sealed interface Criterion {
        /*
         * The preference is determined by the length of the command string
         * when printed using the provided printer.
         */
        data class ShortestPath(
            val printer: CommandPrinter,
        ) : Criterion

        /*
         * The preference is determined by the number of commands in the
         * command list.
         */
        data object FewestCommands : Criterion
    }
}
