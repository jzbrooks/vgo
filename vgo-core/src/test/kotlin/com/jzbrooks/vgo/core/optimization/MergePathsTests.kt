package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.index
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.util.element.createGraphic
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.element.traverseBottomUp
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class MergePathsTests {
    @Test
    fun testMergeSeveralPathsIntoOne() {
        val paths =
            listOf(
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))),
                ),
            )

        val graphic = createGraphic(paths)
        val optimization = MergePaths()

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).index(0).isEqualTo(
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))),
                ),
            ),
        )
    }

    @Test
    fun testMergeGroupPaths() {
        val paths =
            listOf(
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))),
                ),
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                        SmoothCubicBezierCurve(
                            CommandVariant.RELATIVE,
                            listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f))),
                        ),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    ),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                    strokeWidth = 5f,
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))),
                ),
            )

        val group = Group(paths)
        val graphic = createGraphic(listOf(group))
        val optimization = MergePaths()

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).containsExactly(
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                            SmoothCubicBezierCurve(
                                CommandVariant.RELATIVE,
                                listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f))),
                            ),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        ),
                    ),
                    createPath(
                        listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                        strokeWidth = 5f,
                    ),
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                        ),
                    ),
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun testMergePaths() {
        val paths =
            listOf(
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))),
                ),
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                        SmoothCubicBezierCurve(
                            CommandVariant.RELATIVE,
                            listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f))),
                        ),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    ),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                    strokeWidth = 5f,
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))),
                ),
            )

        val graphic = createGraphic(paths)
        val optimization = MergePaths()

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).containsExactly(
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                    SmoothCubicBezierCurve(
                        CommandVariant.RELATIVE,
                        listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f))),
                    ),
                    LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                ),
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                strokeWidth = 5f,
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                ),
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))),
                ),
            ),
        )
    }

    @Test
    fun testMergePathsWithMixedElements() {
        val paths =
            listOf(
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))),
                ),
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                        SmoothCubicBezierCurve(
                            CommandVariant.RELATIVE,
                            listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f))),
                        ),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    ),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                    strokeWidth = 5f,
                ),
                Group(emptyList()),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))),
                ),
            )

        val graphic = createGraphic(paths)
        val optimization = MergePaths()

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).containsExactly(
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                    SmoothCubicBezierCurve(
                        CommandVariant.RELATIVE,
                        listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f))),
                    ),
                    LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                ),
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                strokeWidth = 5f,
            ),
            Group(emptyList()),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                ),
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))),
                ),
            ),
        )
    }

    @Test
    fun testOnlyMergeAppropriateGroups() {
        val paths =
            listOf(
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))),
                ),
                createPath(
                    listOf<Command>(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f)))),
                    fill = Color(0xffaabbccu),
                ),
                createPath(
                    listOf<Command>(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f)))),
                    fill = Color(0xffaabbccu),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))),
                ),
            )

        val graphic = createGraphic(paths)
        val optimization = MergePaths()

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).containsExactly(
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                ),
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f))),
                ),
                fill = Color(0xffaabbccu),
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                ),
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))),
                ),
            ),
        )
    }
}
