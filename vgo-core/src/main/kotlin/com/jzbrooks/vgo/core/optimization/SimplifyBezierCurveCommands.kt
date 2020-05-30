package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.util.math.Point
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Convert curves into shorter commands where possible
 */
class SimplifyBezierCurveCommands(private val tolerance: Float) : TopDownOptimization, PathElementVisitor {
    private var skipAnother = false

    override fun visit(pathElement: PathElement) {
        val commandCount = pathElement.commands.size

        if (commandCount > 0) {
            val commands = mutableListOf<Command>((pathElement.commands.first() as MoveTo).copy())
            val existingCommands = pathElement.commands.drop(1)

            loop@ for ((index, current) in existingCommands.withIndex()) {
                if (skipAnother) {
                    skipAnother = false
                    continue
                }

                assert((current as? ParameterizedCommand<*>)?.variant != CommandVariant.ABSOLUTE)

                val lastAdded = commands.last() as? ParameterizedCommand<*>

                if (current is CubicBezierCurve) {
                    val currentFinalParameter = current.parameters.last()

                    if (current.isStraightLine()) {
                        commands.add(LineTo(current.variant, listOf(currentFinalParameter.end)))

                        val next = existingCommands.getOrNull(index + 1)
                        if (next is ShortcutCubicBezierCurve) {
                            commands.add(CubicBezierCurve(next.variant, next.parameters.map {
                                CubicBezierCurve.Parameter(currentFinalParameter.end - currentFinalParameter.endControl, it.endControl, it.end)
                            }))
                            skipAnother = true
                        }

                        continue@loop
                    }
                    if (lastAdded is CubicBezierCurve &&
                            currentFinalParameter.startControl == (lastAdded.parameters.last().run { end - endControl })) {
                        commands.add(ShortcutCubicBezierCurve(current.variant, current.parameters.map {
                            ShortcutCubicBezierCurve.Parameter(it.endControl, it.end)
                        }))
                        continue@loop
                    }
                    if (lastAdded is ShortcutCubicBezierCurve &&
                            currentFinalParameter.startControl == (lastAdded.parameters.last().run { end - endControl })) {
                        commands.add(ShortcutCubicBezierCurve(current.variant, current.parameters.map {
                            ShortcutCubicBezierCurve.Parameter(it.endControl, it.end)
                        }))
                        continue@loop
                    }
                    if (lastAdded !is ShortcutCubicBezierCurve && lastAdded !is CubicBezierCurve &&
                            currentFinalParameter.startControl == Point.ZERO) {
                        commands.add(ShortcutCubicBezierCurve(current.variant, current.parameters.map {
                            ShortcutCubicBezierCurve.Parameter(it.endControl, it.end)
                        }))
                        continue@loop
                    }
                }

                if (current is ShortcutCubicBezierCurve && current.isStraightLine()) {
                    val currentFinalParameter = current.parameters.last()

                    commands.add(LineTo(current.variant, listOf(currentFinalParameter.end)))

                    val next = existingCommands.getOrNull(index + 1)
                    if (next is ShortcutCubicBezierCurve) {
                        commands.add(CubicBezierCurve(next.variant, next.parameters.map {
                            CubicBezierCurve.Parameter(currentFinalParameter.end - currentFinalParameter.endControl, it.endControl, it.end)
                        }))
                        skipAnother = true
                    }

                    continue@loop
                }

                if (current is QuadraticBezierCurve) {
                    val currentFinalParameter = current.parameters.last()
                    if (current.isStraightLine()) {
                        commands.add(LineTo(current.variant, listOf(currentFinalParameter.end)))

                        val next = existingCommands.getOrNull(index + 1)
                        if (next is ShortcutQuadraticBezierCurve) {
                            commands.add(QuadraticBezierCurve(next.variant, next.parameters.map {
                                QuadraticBezierCurve.Parameter(currentFinalParameter.end - currentFinalParameter.control, it)
                            }))
                            skipAnother = true
                        }

                        continue@loop
                    }

                    if (lastAdded is QuadraticBezierCurve &&
                            currentFinalParameter.control == (lastAdded.parameters.last().run { end - control })) {
                        commands.add(ShortcutQuadraticBezierCurve(current.variant, current.parameters.map {
                            it.end
                        }))
                        continue@loop
                    }

                    if (lastAdded is ShortcutQuadraticBezierCurve &&
                            currentFinalParameter.end == lastAdded.parameters.last()) {
                        commands.add(ShortcutQuadraticBezierCurve(current.variant, current.parameters.map {
                            it.end
                        }))
                        continue@loop
                    }
                }

                if (current is ShortcutQuadraticBezierCurve &&
                        lastAdded !is QuadraticBezierCurve &&
                        lastAdded !is ShortcutQuadraticBezierCurve) {
                    val currentFinalParameter = current.parameters.last()
                    commands.add(LineTo(current.variant, listOf(currentFinalParameter)))
                    continue@loop
                }
                commands.add(current)
            }

            pathElement.commands = commands
        }
    }

    private fun CubicBezierCurve.isStraightLine(): Boolean {
        val lastParameter = parameters.last().end
        val a = -lastParameter.y.toDouble()
        val b = lastParameter.x.toDouble()
        val d = 1 / (a * a + b * b)

        if (!d.isFinite()) {
            return false
        }

        if (sqrt((a * parameters.last().endControl.x.toDouble() + b * parameters.last().endControl.y.toDouble()).pow(2) * d) > tolerance) {
            return false
        }

        if (sqrt((a * parameters.last().startControl.x.toDouble() + b * parameters.last().startControl.y.toDouble()).pow(2) * d) > tolerance) {
            return false
        }

        return true
    }

    private fun ShortcutCubicBezierCurve.isStraightLine(): Boolean {
        val lastParameter = parameters.last().end
        val a = -lastParameter.y.toDouble()
        val b = lastParameter.x.toDouble()
        val d = 1 / (a * a + b * b)

        if (!d.isFinite()) {
            return false
        }

        if (sqrt((a * parameters.last().endControl.x.toDouble() + b * parameters.last().endControl.y.toDouble()).pow(2) * d) > tolerance) {
            return false
        }

        return true
    }

    private fun QuadraticBezierCurve.isStraightLine(): Boolean {
        val lastParameter = parameters.last().end
        val a = -lastParameter.y.toDouble()
        val b = lastParameter.x.toDouble()
        val d = 1 / (a * a + b * b)

        if (!d.isFinite()) {
            return false
        }

        if (sqrt((a * parameters.last().control.x.toDouble() + b * parameters.last().control.y.toDouble()).pow(2) * d) > tolerance) {
            return false
        }

        return true
    }
}