package com.jzbrooks.guacamole.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import com.jzbrooks.guacamole.graphic.*
import com.jzbrooks.guacamole.graphic.command.*
import com.jzbrooks.guacamole.graphic.command.CommandVariant
import org.junit.Test

class MergePathsTests {

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
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mapOf("android:strokeWidth" to "1")),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val group = Group(paths)

        val optimization = MergePaths()
        optimization.visit(group)

        assertThat(group.elements).hasSize(3)
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
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mapOf("android:strokeWidth" to "1")),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var metadata = emptyMap<String, String>()
        }

        val optimization = MergePaths()
        optimization.visit(graphic)

        assertThat(graphic.elements).hasSize(3)
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
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), mapOf("android:strokeWidth" to "1")),
                Group(emptyList()),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var metadata = emptyMap<String, String>()
        }

        val optimization = MergePaths()
        optimization.visit(graphic)

        assertThat(graphic.elements).hasSize(4)
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
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var metadata = emptyMap<String, String>()
        }

        val optimization = MergePaths()
        optimization.visit(graphic)

        assertThat(graphic.elements).hasSize(3)
    }

    @Test
    fun testMergePathsWithMetadataAfterMergedPathElement() {
        val paths = listOf(
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))),
                ClipPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))))),
                ClipPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f))))),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))), mapOf("android:fillColor" to "#FF0011FF")),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var metadata = emptyMap<String, String>()
        }

        val optimization = MergePaths()
        optimization.visit(graphic)

        assertThat(graphic.elements).hasSize(4)
    }
}