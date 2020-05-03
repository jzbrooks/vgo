package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.util.math.fitCircle
import com.jzbrooks.vgo.core.util.math.isConvex

class ConvertCurvesToArcs(private val printer: CommandPrinter): TopDownOptimization, PathElementVisitor {
    private lateinit var newCommands: MutableList<Command>

    override fun visit(pathElement: PathElement) {
        newCommands = ArrayList(pathElement.commands.size)

        for (i in pathElement.commands.indices) {
            val currentInstruction = pathElement.commands[i]
            val previousInstruction = pathElement.commands.getOrNull(i - 1)

            val newCommand = when (currentInstruction) {
                is CubicBezierCurve -> adapt(currentInstruction)
                is ShortcutCubicBezierCurve -> adapt(currentInstruction)
                else -> currentInstruction
            }

            newCommands.add(newCommand)
        }

        pathElement.commands = newCommands
    }

    private fun adapt(cubic: CubicBezierCurve): Command {
        val circle = cubic.fitCircle()
        if (cubic.isConvex() && circle != null) {
            val (startControl, _, end) = cubic.parameters[0]
            // todo: do we need to round this?
            val r = circle.radius
            val sweep = if ((end.y * startControl.x - end.x * startControl.y) > 0) {
                EllipticalArcCurve.SweepFlag.CLOCKWISE
            } else {
                EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
            }
            val arc = EllipticalArcCurve(
                    cubic.variant,
                    listOf(
                            EllipticalArcCurve.Parameter(r, r, 0f, EllipticalArcCurve.ArcFlag.SMALL, sweep, end)
                    )
            )

            return if (printer.print(arc).length < printer.print(cubic).length) {
                arc
            } else {
                cubic
            }
        }

        return cubic
    }

    private fun adapt(shortcutCubic: ShortcutCubicBezierCurve): Command {
        val circle = shortcutCubic.fitCircle()
        if (shortcutCubic.isConvex() && circle != null) {
            val (_, end) = shortcutCubic.parameters[0]
            // todo: do we need to round this?
            val r = circle.radius
            val sweep = EllipticalArcCurve.SweepFlag.ANTICLOCKWISE
            val arc = EllipticalArcCurve(
                    shortcutCubic.variant,
                    listOf(
                            EllipticalArcCurve.Parameter(r, r, 0f, EllipticalArcCurve.ArcFlag.SMALL, sweep, end)
                    )
            )

            return if (printer.print(arc).length < printer.print(shortcutCubic).length) {
                arc
            } else {
                shortcutCubic
            }
        }

        return shortcutCubic
    }
}