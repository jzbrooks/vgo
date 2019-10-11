package com.jzbrooks.guacamole.svg

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.jzbrooks.guacamole.assertk.extensions.containsKey
import com.jzbrooks.guacamole.assertk.extensions.containsKeys
import com.jzbrooks.guacamole.assertk.extensions.doesNotContainKey
import com.jzbrooks.guacamole.core.graphic.Extra
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.command.*
import com.jzbrooks.guacamole.core.util.math.Point
import org.junit.Test
import java.io.ByteArrayInputStream

class ScalableVectorGraphicReaderTests {
    @Test
    fun testParseDimensions() {
        javaClass.getResourceAsStream("/simple_heart.svg").use {
            val graphic: Graphic = parse(it)

            assertThat(graphic.attributes["viewBox"]).isEqualTo("0 0 100 100")
        }
    }

    @Test
    fun testParseMetadataDoesNotContainPathData() {
        javaClass.getResourceAsStream("/simple_heart.svg").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.attributes).doesNotContainKey("d")
        }
    }

    @Test
    fun testParseMetadata() {
        javaClass.getResourceAsStream("/simple_heart.svg").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.attributes).containsKeys("stroke", "fill")
        }
    }

    @Test
    fun testParsePaths() {
        javaClass.getResourceAsStream("/simple_heart.svg").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.commands).isEqualTo(
                    listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 30f))),
                            EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(EllipticalArcCurve.Parameter(20f, 20f, 0f, EllipticalArcCurve.ArcFlag.SMALL, EllipticalArcCurve.SweepFlag.CLOCKWISE, Point(50f, 30f)))),
                            EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(EllipticalArcCurve.Parameter(20f, 20f, 0f, EllipticalArcCurve.ArcFlag.SMALL, EllipticalArcCurve.SweepFlag.CLOCKWISE, Point(90f, 30f)))),
                            QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(90f, 60f), Point(50f, 90f)))),
                            QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(10f, 60f), Point(10f, 30f)))),
                            ClosePath()
                    )
            )
            assertThat(graphic.elements).hasSize(1)
        }
    }

    @Test
    fun testStoreIdForPath() {
        javaClass.getResourceAsStream("/simple_heart.svg").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.attributes).containsKey("id")
            assertThat(path.attributes["id"]).isEqualTo("heart")
        }
    }

    @Test
    fun testParseComment() {
        ByteArrayInputStream("<svg><!-- test comment --></svg>".toByteArray()).use {
            val graphic: Graphic = parse(it)

            val unknown = graphic.elements.first() as Extra

            assertThat(unknown.name).isEqualTo(" test comment ")
            assertThat(unknown.elements).isEmpty()
        }
    }

    @Test
    fun testParseSelfClosedUnknownElementWithoutChildren() {
        ByteArrayInputStream("<svg><bicycle /></svg>".toByteArray()).use {
            val graphic: Graphic = parse(it)

            val unknown = graphic.elements.first() as Extra

            assertThat(unknown.name).isEqualTo("bicycle")
            assertThat(unknown.elements).isEmpty()
        }
    }

    @Test
    fun testParseUnknownElementWithoutChildren() {
        ByteArrayInputStream("<svg><bicycle></bicycle></svg>".toByteArray()).use {
            val graphic: Graphic = parse(it)

            val unknown = graphic.elements.first() as Extra

            assertThat(unknown.name).isEqualTo("bicycle")
            assertThat(unknown.elements).isEmpty()
        }
    }

    @Test
    fun testParseUnknownElementWithChildren() {
        val vectorText = """
            |<svg>
            |  <bicycle>
            |    <path d="M0,0l2,3Z" />
            |  </bicycle>
            |</svg>
            |""".trimMargin().toByteArray()

        val expectedChild = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f,0f))), LineTo(CommandVariant.RELATIVE, listOf(Point(2f, 3f))), ClosePath()))
        ByteArrayInputStream(vectorText).use {
            val graphic: Graphic = parse(it)

            val unknown = graphic.elements.first() as Extra

            assertThat(unknown.name).isEqualTo("bicycle")
            assertThat(unknown.elements).containsExactly(expectedChild)
        }
    }
}
