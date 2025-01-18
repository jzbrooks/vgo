package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.util.math.Point
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Convert curves into shorter commands where possible
 */
class SimplifyBezierCurveCommands(
    private val tolerance: Float,
) : TopDownTransformation {
    private var skipAnother = false

    override fun visit(graphic: Graphic) {}

    override fun visit(clipPath: ClipPath) {}

    override fun visit(group: Group) {}

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {
        if (path.commands.isEmpty()) return

        val commands = mutableListOf<Command>((path.commands.first() as MoveTo).copy())
        val existingCommands = path.commands.drop(1)

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
                    if (next is SmoothCubicBezierCurve) {
                        commands.add(
                            CubicBezierCurve(
                                next.variant,
                                next.parameters.map {
                                    CubicBezierCurve.Parameter(
                                        currentFinalParameter.end - currentFinalParameter.endControl,
                                        it.endControl,
                                        it.end,
                                    )
                                },
                            ),
                        )
                        skipAnother = true
                    }

                    continue@loop
                }
                if (lastAdded is CubicBezierCurve &&
                    currentFinalParameter.startControl == (lastAdded.parameters.last().run { end - endControl })
                ) {
                    commands.add(
                        SmoothCubicBezierCurve(
                            current.variant,
                            current.parameters.map {
                                SmoothCubicBezierCurve.Parameter(it.endControl, it.end)
                            },
                        ),
                    )
                    continue@loop
                }
                if (lastAdded is SmoothCubicBezierCurve &&
                    currentFinalParameter.startControl == (lastAdded.parameters.last().run { end - endControl })
                ) {
                    commands.add(
                        SmoothCubicBezierCurve(
                            current.variant,
                            current.parameters.map {
                                SmoothCubicBezierCurve.Parameter(it.endControl, it.end)
                            },
                        ),
                    )
                    continue@loop
                }
                if (lastAdded !is SmoothCubicBezierCurve &&
                    lastAdded !is CubicBezierCurve &&
                    currentFinalParameter.startControl == Point.ZERO
                ) {
                    commands.add(
                        SmoothCubicBezierCurve(
                            current.variant,
                            current.parameters.map {
                                SmoothCubicBezierCurve.Parameter(it.endControl, it.end)
                            },
                        ),
                    )
                    continue@loop
                }
            }

            if (current is SmoothCubicBezierCurve && current.isStraightLine()) {
                val currentFinalParameter = current.parameters.last()

                commands.add(LineTo(current.variant, listOf(currentFinalParameter.end)))

                val next = existingCommands.getOrNull(index + 1)
                if (next is SmoothCubicBezierCurve) {
                    commands.add(
                        CubicBezierCurve(
                            next.variant,
                            next.parameters.map {
                                CubicBezierCurve.Parameter(
                                    currentFinalParameter.end - currentFinalParameter.endControl,
                                    it.endControl,
                                    it.end,
                                )
                            },
                        ),
                    )
                    skipAnother = true
                }

                continue@loop
            }

            if (current is QuadraticBezierCurve) {
                val currentFinalParameter = current.parameters.last()
                if (current.isStraightLine()) {
                    commands.add(LineTo(current.variant, listOf(currentFinalParameter.end)))

                    val next = existingCommands.getOrNull(index + 1)
                    if (next is SmoothQuadraticBezierCurve) {
                        commands.add(
                            QuadraticBezierCurve(
                                next.variant,
                                next.parameters.map {
                                    QuadraticBezierCurve.Parameter(currentFinalParameter.end - currentFinalParameter.control, it)
                                },
                            ),
                        )
                        skipAnother = true
                    }

                    continue@loop
                }

                if (lastAdded is QuadraticBezierCurve &&
                    currentFinalParameter.control == (lastAdded.parameters.last().run { end - control })
                ) {
                    commands.add(
                        SmoothQuadraticBezierCurve(
                            current.variant,
                            current.parameters.map {
                                it.end
                            },
                        ),
                    )
                    continue@loop
                }

                if (lastAdded is SmoothQuadraticBezierCurve &&
                    currentFinalParameter.end == lastAdded.parameters.last()
                ) {
                    commands.add(
                        SmoothQuadraticBezierCurve(
                            current.variant,
                            current.parameters.map {
                                it.end
                            },
                        ),
                    )
                    continue@loop
                }
            }

            if (current is SmoothQuadraticBezierCurve &&
                lastAdded !is QuadraticBezierCurve &&
                lastAdded !is SmoothQuadraticBezierCurve
            ) {
                val currentFinalParameter = current.parameters.last()
                commands.add(LineTo(current.variant, listOf(currentFinalParameter)))
                continue@loop
            }
            commands.add(current)
        }

        path.commands = commands
    }

    private fun CubicBezierCurve.isStraightLine(): Boolean {
        val lastParameter = parameters.last().end
        val a = -lastParameter.y.toDouble()
        val b = lastParameter.x.toDouble()
        val d = 1 / (a * a + b * b)

        if (!d.isFinite()) {
            return false
        }

        if (sqrt(
                (
                    a *
                        parameters
                            .last()
                            .endControl.x
                            .toDouble() + b *
                        parameters
                            .last()
                            .endControl.y
                            .toDouble()
                ).pow(2) * d,
            ) > tolerance
        ) {
            return false
        }

        if (sqrt(
                (
                    a *
                        parameters
                            .last()
                            .startControl.x
                            .toDouble() + b *
                        parameters
                            .last()
                            .startControl.y
                            .toDouble()
                ).pow(2) * d,
            ) > tolerance
        ) {
            return false
        }

        return true
    }

    private fun SmoothCubicBezierCurve.isStraightLine(): Boolean {
        val lastParameter = parameters.last().end
        val a = -lastParameter.y.toDouble()
        val b = lastParameter.x.toDouble()
        val d = 1 / (a * a + b * b)

        if (!d.isFinite()) {
            return false
        }

        if (sqrt(
                (
                    a *
                        parameters
                            .last()
                            .endControl.x
                            .toDouble() + b *
                        parameters
                            .last()
                            .endControl.y
                            .toDouble()
                ).pow(2) * d,
            ) > tolerance
        ) {
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

        if (sqrt(
                (
                    a *
                        parameters
                            .last()
                            .control.x
                            .toDouble() + b *
                        parameters
                            .last()
                            .control.y
                            .toDouble()
                ).pow(2) * d,
            ) > tolerance
        ) {
            return false
        }

        return true
    }
}
