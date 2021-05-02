package com.jzbrooks.vgo.svg

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.util.assertk.containsKey
import com.jzbrooks.vgo.util.assertk.containsKeys
import com.jzbrooks.vgo.util.assertk.doesNotContainKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class ScalableVectorGraphicReaderTests {
    private lateinit var node: Node

    @BeforeEach
    fun setup() {
        javaClass.getResourceAsStream("/simple_heart.svg").use { input ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
            document.normalize()
            node = document.firstChild
        }
    }
    @Test
    fun testParseDimensions() {
        val graphic: Graphic = parse(node)

        assertThat(graphic.attributes["viewBox"]).isEqualTo("0 0 100 100")
    }

    @Test
    fun testParseMetadataDoesNotContainPathData() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path.attributes).doesNotContainKey("d")
    }

    @Test
    fun testParseMetadata() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path.attributes).containsKeys("stroke", "fill")
    }

    @Test
    fun testParsePaths() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path.commands).isEqualTo(
            listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 30f))),
                EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(EllipticalArcCurve.Parameter(20f, 20f, 0f, EllipticalArcCurve.ArcFlag.SMALL, EllipticalArcCurve.SweepFlag.CLOCKWISE, Point(50f, 30f)))),
                EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(EllipticalArcCurve.Parameter(20f, 20f, 0f, EllipticalArcCurve.ArcFlag.SMALL, EllipticalArcCurve.SweepFlag.CLOCKWISE, Point(90f, 30f)))),
                QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(90f, 60f), Point(50f, 90f)))),
                QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(10f, 60f), Point(10f, 30f)))),
                ClosePath
            )
        )
        assertThat(graphic.elements).hasSize(1)
    }

    @Test
    fun testStoreIdForPath() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path.attributes).containsKey("id")
        assertThat(path.attributes["id"]).isEqualTo("heart")
    }

    @Test
    fun testIgnoreComment() {
        val commentDocument = ByteArrayInputStream("<svg><!-- test comment --></svg>".toByteArray()).use { input ->
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                documentElement.normalize()
            }
        }

        val graphic: Graphic = parse(commentDocument.firstChild)

        assertThat(graphic.elements).isEmpty()
    }

    @Test
    fun testParseSelfClosedUnknownElementWithoutChildren() {
        val unknownElementDocument = ByteArrayInputStream("<svg><bicycle /></svg>".toByteArray()).use { input ->
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                documentElement.normalize()
            }
        }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown.name).isEqualTo("bicycle")
        assertThat(unknown.elements).isEmpty()
    }

    @Test
    fun testParseUnknownElementWithoutChildren() {
        val unknownElementDocument = ByteArrayInputStream("<svg><bicycle></bicycle></svg>".toByteArray()).use { input ->
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                documentElement.normalize()
            }
        }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown.name).isEqualTo("bicycle")
        assertThat(unknown.elements).isEmpty()
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

        val expectedChild = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))), LineTo(CommandVariant.RELATIVE, listOf(Point(2f, 3f))), ClosePath))

        val unknownElementDocument = ByteArrayInputStream(vectorText).use { input ->
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                normalize()
            }
        }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown.name).isEqualTo("bicycle")
        assertThat(unknown.elements).containsExactly(expectedChild)
    }
}
