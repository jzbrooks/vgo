package com.jzbrooks.avdo.optimization

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.avdo.graphic.*
import com.jzbrooks.avdo.graphic.command.*
import com.jzbrooks.avdo.graphic.command.CommandVariant
import org.junit.Test

class MergeGraphicPathsTests {
    @Test
    fun testMergePaths() {
        val paths = listOf(
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f)))), 1),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))), 1),
                Path(
                        listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                                ShortcutCubicBezierCurve(CommandVariant.RELATIVE, listOf(ShortcutCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                        ),
                        1),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))), 1, mapOf("android:strokeWidth" to "1")),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))), 1),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))), 1)
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var metadata = emptyMap<String, String>()
        }

        val optimization = MergeGraphicPaths()
        optimization.visit(graphic)

        assertThat(graphic.elements.size).isEqualTo(3)
    }
}