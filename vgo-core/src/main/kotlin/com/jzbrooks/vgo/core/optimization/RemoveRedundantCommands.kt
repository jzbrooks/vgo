package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.computeAbsoluteCoordinates
import kotlin.math.abs

/**
 * Elide commands that don't contribute to the overall graphic
 */
class RemoveRedundantCommands : TopDownOptimization, PathElementVisitor {
    override fun visit(pathElement: PathElement) {
        val commandCount = pathElement.commands.size

        if (commandCount > 0) {
            val firstCommand = pathElement.commands.first() as MoveTo

            val commands = mutableListOf<Command>(firstCommand)
            loop@ for (current in pathElement.commands.drop(1)) {
                assert((current as? ParameterizedCommand<*>)?.variant != CommandVariant.ABSOLUTE)

                when (current) {
                    is LineTo -> if (current.parameters.all { it == Point.ZERO }) { continue@loop }
                    is VerticalLineTo -> if (current.parameters.all { it == 0.0f }) { continue@loop }
                    is HorizontalLineTo -> if (current.parameters.all { it == 0.0f }) { continue@loop }
                    is CubicBezierCurve -> if (current.parameters.all { it == CubicBezierCurve.Parameter(Point(0f, 0f), Point(0f, 0f), Point(0f, 0f)) }) { continue@loop }
                    is SmoothCubicBezierCurve -> if (current.parameters.all { it == SmoothCubicBezierCurve.Parameter(Point(0f, 0f), Point(0f, 0f)) }) { continue@loop }
                    is QuadraticBezierCurve -> if (current.parameters.all { it == QuadraticBezierCurve.Parameter(Point(0f, 0f), Point(0f, 0f)) }) { continue@loop }
                    is SmoothQuadraticBezierCurve -> if (current.parameters.all { it == Point.ZERO }) { continue@loop }
                    is EllipticalArcCurve -> if (current.parameters.all { (abs(it.radiusX) < 1e-7f && abs(it.radiusY) < 1e-7) || it.end == Point.ZERO }) { continue@loop }
                }

                commands.add(current)
            }

            if (pathElement.commands.last() is ClosePath) {
                val commandsWithoutFinalClosePath = commands.dropLast(1)
                val current = computeAbsoluteCoordinates(commandsWithoutFinalClosePath)
                val firstCurrentPoint = firstCommand.parameters.last()

                if (current == firstCurrentPoint) {
                    pathElement.commands = commandsWithoutFinalClosePath
                    return
                }
            }

            pathElement.commands = commands
        }
    }
}
