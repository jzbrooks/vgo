package com.jzbrooks.vgo.core.transformation

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.first
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.FakeCommandPrinter
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
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
        val optimization = MergePaths(MergePaths.Constraints.None)

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).containsExactly(
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                ),
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))),
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
        val optimization = MergePaths(MergePaths.Constraints.None)

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).containsExactly(
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        ),
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
        val optimization = MergePaths(MergePaths.Constraints.None)

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
        val optimization = MergePaths(MergePaths.Constraints.None)

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
        val optimization = MergePaths(MergePaths.Constraints.None)

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

    @Test
    fun `overlapping paths are not merged`() {
        // M 10,30
        // A 20,20 0,0,1 50,30
        // A 20,20 0,0,1 90,30
        // Q 90,60 50,90
        // Q 10,60 10,30
        val firstHeart =
            createPath(
                listOf<Command>(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 30f))),
                    EllipticalArcCurve(
                        CommandVariant.ABSOLUTE,
                        listOf(
                            EllipticalArcCurve.Parameter(
                                20f,
                                20f,
                                0f,
                                EllipticalArcCurve.ArcFlag.SMALL,
                                EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                Point(50f, 30f),
                            ),
                        ),
                    ),
                    EllipticalArcCurve(
                        CommandVariant.ABSOLUTE,
                        listOf(
                            EllipticalArcCurve.Parameter(
                                20f,
                                20f,
                                0f,
                                EllipticalArcCurve.ArcFlag.SMALL,
                                EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                Point(90f, 30f),
                            ),
                        ),
                    ),
                    QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(90f, 60f), Point(50f, 90f)))),
                    QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(10f, 60f), Point(10f, 30f)))),
                ),
            )

        // M 20,40
        // A 20,20 0,0,1 60,40
        // A 20,20 0,0,1 100,40
        // Q 100,70 60,100
        // Q 20,70 20,40
        val offsetHeart =
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))),
                    EllipticalArcCurve(
                        CommandVariant.ABSOLUTE,
                        listOf(
                            EllipticalArcCurve.Parameter(
                                20f,
                                20f,
                                0f,
                                EllipticalArcCurve.ArcFlag.SMALL,
                                EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                Point(60f, 40f),
                            ),
                        ),
                    ),
                    EllipticalArcCurve(
                        CommandVariant.ABSOLUTE,
                        listOf(
                            EllipticalArcCurve.Parameter(
                                20f,
                                20f,
                                0f,
                                EllipticalArcCurve.ArcFlag.SMALL,
                                EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                Point(100f, 40f),
                            ),
                        ),
                    ),
                    QuadraticBezierCurve(
                        CommandVariant.ABSOLUTE,
                        listOf(QuadraticBezierCurve.Parameter(Point(100f, 70f), Point(60f, 100f))),
                    ),
                    QuadraticBezierCurve(
                        CommandVariant.ABSOLUTE,
                        listOf(QuadraticBezierCurve.Parameter(Point(20f, 70f), Point(20f, 40f))),
                    ),
                ),
            )

        val graphic = createGraphic(listOf(firstHeart, offsetHeart))
        val optimization = MergePaths(MergePaths.Constraints.None)

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).hasSize(2)
    }

    @Test
    fun pathLengthConstraints() {
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
        val optimization = MergePaths(MergePaths.Constraints.PathLength(FakeCommandPrinter(), 16))

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).containsExactly(
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                ),
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))),
            ),
        )
    }

    @Test
    fun mergedPathsInitialCommandIsMadeAbsolute() {
        val paths =
            listOf(
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.RELATIVE, listOf(Point(10f, 10f), Point(10f, 10f)))),
                ),
            )

        val graphic = createGraphic(paths)
        val optimization = MergePaths(MergePaths.Constraints.None)

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements)
            .first()
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .index(1)
            .isInstanceOf<ParameterizedCommand<*>>()
            .all {
                prop(ParameterizedCommand<*>::variant).isEqualTo(CommandVariant.ABSOLUTE)
                prop(ParameterizedCommand<*>::variant.name) { it.parameters }
                    .isEqualTo(listOf(Point(10f, 10f), Point(20f, 20f)))
            }
    }

    @Test
    fun mergedPathsInitialCommandIsMadeAbsoluteBeforeConstraints() {
        // This would be merged if directly considered by constraints (merged length is 15)
        // M0,0
        // m10,10 1,1 -> M0,0 m10,10 1, 1

        // When the relative command is made absolute for merging, the merged path would
        // be longer (17 chars) than the constraint.
        // M0,0
        // M10,10 11,11 -> M0,0 M10,10 11,11
        val paths =
            listOf(
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f)))),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.RELATIVE, listOf(Point(10f, 10f), Point(1f, 1f), Point(1f, 1f)))),
                ),
            )

        val graphic = createGraphic(paths)
        val optimization = MergePaths(MergePaths.Constraints.PathLength(FakeCommandPrinter(), 16))

        traverseBottomUp(graphic) { it.accept(optimization) }

        assertThat(graphic::elements).hasSize(2)
    }
}
