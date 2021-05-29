package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.computeAbsoluteCoordinates
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * Elide commands that don't contribute to the overall graphic
 */
class RemoveRedundantCommands : TopDownOptimization, PathElementVisitor {
    override fun visit(pathElement: PathElement) {

        if (pathElement.commands.isNotEmpty()) {
            val firstCommand = pathElement.commands.first() as MoveTo

            val commands = mutableListOf<Command>(firstCommand)
            for (current in pathElement.commands.drop(1)) {
                assert((current as? ParameterizedCommand<*>)?.variant != CommandVariant.ABSOLUTE)

                when (current) {
                    is MoveTo -> if (current.parameters.reduce(Point::plus).isApproximately(Point.ZERO)) continue
                    is LineTo -> if (current.parameters.all { it.isApproximately(Point.ZERO) }) continue
                    is VerticalLineTo -> if (current.parameters.all { it.absoluteValue < 1e-3f }) continue
                    is HorizontalLineTo -> if (current.parameters.all { it.absoluteValue < 1e-3f }) continue
                    is CubicBezierCurve -> if (current.parameters.map(CubicBezierCurve.Parameter::end).all { it.isApproximately(Point.ZERO) }) continue
                    is SmoothCubicBezierCurve -> if (current.parameters.map(SmoothCubicBezierCurve.Parameter::end).all { it.isApproximately(Point.ZERO) }) continue
                    is QuadraticBezierCurve -> if (current.parameters.map(QuadraticBezierCurve.Parameter::end).all { it.isApproximately(Point.ZERO) }) continue
                    is SmoothQuadraticBezierCurve -> if (current.parameters.all { it.isApproximately(Point.ZERO) }) continue
                    is EllipticalArcCurve -> if (current.parameters.all { (abs(it.radiusX) < 1e-3f && abs(it.radiusY) < 1e-3f) || it.end.isApproximately(Point.ZERO) }) continue
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
