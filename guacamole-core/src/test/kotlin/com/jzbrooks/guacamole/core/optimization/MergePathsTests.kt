package com.jzbrooks.guacamole.core.optimization

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.guacamole.core.graphic.*
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.graphic.command.LineTo
import com.jzbrooks.guacamole.core.graphic.command.MoveTo
import com.jzbrooks.guacamole.core.graphic.command.ShortcutCubicBezierCurve
import com.jzbrooks.guacamole.core.util.math.Point
import org.junit.Test

class MergePathsTests {

    @Test
    fun testMergeSeveralPathsIntoOne() {
        val paths = listOf(
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var attributes = mutableMapOf<String, String>()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements.first()).isEqualTo(Path(listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
        )))
    }

    @Test
    fun testMergeGroupPaths() {
        val paths = listOf(
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))),
                Path(
                        listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                                ShortcutCubicBezierCurve(CommandVariant.RELATIVE, listOf(ShortcutCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                        )
                ),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mutableMapOf("android:strokeWidth" to "1")),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val group = Group(paths)

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var attributes = mutableMapOf<String, String>()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(listOf(
                Group(listOf(
                        Path(listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                                ShortcutCubicBezierCurve(CommandVariant.RELATIVE, listOf(ShortcutCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                        )),
                        Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mutableMapOf("android:strokeWidth" to "1")),
                        Path(listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                        ))
                ))
        ))
    }

    @Test
    fun testMergePaths() {
        val paths = listOf(
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))),
                Path(
                        listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                                ShortcutCubicBezierCurve(CommandVariant.RELATIVE, listOf(ShortcutCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                        )
                ),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mutableMapOf("android:strokeWidth" to "1")),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var attributes = mutableMapOf<String, String>()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(listOf(
                Path(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                        ShortcutCubicBezierCurve(CommandVariant.RELATIVE, listOf(ShortcutCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                )),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mutableMapOf("android:strokeWidth" to "1")),
                Path(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                ))
        ))
    }

    @Test
    fun testMergePathsWithMixedElements() {
        val paths = listOf(
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))),
                Path(
                        listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                                ShortcutCubicBezierCurve(CommandVariant.RELATIVE, listOf(ShortcutCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                        )
                ),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mutableMapOf("android:strokeWidth" to "1")),
                Group(emptyList()),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var attributes = mutableMapOf<String, String>()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(listOf(
                Path(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                        ShortcutCubicBezierCurve(CommandVariant.RELATIVE, listOf(ShortcutCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                )),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mutableMapOf("android:strokeWidth" to "1")),
                Group(emptyList()),
                Path(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                ))
        ))
    }

    @Test
    fun testMergePathsWithMixedPathElements() {
        val paths = listOf(
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))),
                ClipPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))))),
                ClipPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var attributes = mutableMapOf<String, String>()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(listOf(
                Path(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                )),
                ClipPath(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f)))
                )),
                Path(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                ))
        ))
    }

    @Test
    fun testMergePathsWithAttributesAfterMergedPathElement() {
        val paths = listOf(
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))),
                ClipPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))))),
                ClipPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))), mutableMapOf("android:fillColor" to "#FF0011FF")),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var attributes = mutableMapOf<String, String>()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(listOf(
                Path(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                )),
                ClipPath(listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f)))
                )),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))), mutableMapOf("android:fillColor" to "#FF0011FF")),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        ))
    }
}