package com.jzbrooks.vgo.svg

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.containsNone
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.util.element.createPath
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

        assertThat(graphic::foreign).contains("viewBox", "0 0 100 100")
    }

    @Test
    fun testParseMetadataDoesNotContainPathData() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path.foreign.keys, "foreign keys").containsNone("d")
    }

    @Test
    fun testParseMetadata() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path::stroke).isEqualTo(Color(0xFFFF0000u))
    }

    @Test
    fun testParsePaths() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path::commands).containsExactly(
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
            ClosePath,
        )
        assertThat(graphic::elements).hasSize(1)
    }

    @Test
    fun testStoreIdForPath() {
        val graphic: Graphic = parse(node)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf(Path::class)
            .prop(Path::id)
            .isEqualTo("heart")
    }

    @Test
    fun testIgnoreComment() {
        val commentDocument =
            ByteArrayInputStream("<svg><!-- test comment --></svg>".toByteArray()).use { input ->
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(commentDocument.firstChild)

        assertThat(graphic::elements).isEmpty()
    }

    @Test
    fun testParseSelfClosedUnknownElementWithoutChildren() {
        val unknownElementDocument =
            ByteArrayInputStream("<svg><bicycle /></svg>".toByteArray()).use { input ->
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        assertThat(graphic::elements).index(0).isInstanceOf(Extra::class).all {
            prop(Extra::name).isEqualTo("bicycle")
            prop(Extra::elements).isEmpty()
        }
    }

    @Test
    fun testParseUnknownElementWithoutChildren() {
        val unknownElementDocument =
            ByteArrayInputStream("<svg><bicycle></bicycle></svg>".toByteArray()).use { input ->
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        assertThat(graphic::elements).index(0).isInstanceOf(Extra::class).all {
            prop(Extra::name).isEqualTo("bicycle")
            prop(Extra::elements).isEmpty()
        }
    }

    @Test
    fun testParseFillFromStyleAttribute() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <path style="fill:#ff0000" d="M0,0l2,3Z" />
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val path = graphic.elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0xFFFF0000u))
    }

    @Test
    fun testParseStrokeFromStyleAttribute() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <path style="stroke:rgb(0,255,0);stroke-width:2" d="M0,0l2,3Z" />
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val path = graphic.elements.first() as Path

        assertThat(path::stroke).isEqualTo(Color(0xFF00FF00u))
        assertThat(path::strokeWidth).isEqualTo(2f)
    }

    @Test
    fun testStyleAttributeWinsOverPresentationAttribute() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <path fill="blue" style="fill:red" d="M0,0l2,3Z" />
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val path = graphic.elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0xFFFF0000u))
    }

    @Test
    fun testStyleAttributeRemovedFromForeign() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <path style="fill:red;opacity:0.5" d="M0,0l2,3Z" />
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val path = graphic.elements.first() as Path

        assertThat(path.foreign.keys, "foreign keys").containsNone("style")
    }

    @Test
    fun testIgnoreUnknownStyleProperties() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <path style="opacity:0.5;display:inline" d="M0,0l2,3Z" />
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val path = graphic.elements.first() as Path

        assertThat(path::fill).isEqualTo(com.jzbrooks.vgo.core.Colors.BLACK)
        assertThat(path::stroke).isEqualTo(com.jzbrooks.vgo.core.Colors.TRANSPARENT)
    }

    @Test
    fun testPathInheritsFillFromGroup() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <g fill="#ffffff">
            |    <path d="M0,0l2,3Z" />
            |  </g>
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val group = graphic.elements.first() as Group
        val path = group.elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0xFFFFFFFFu))
    }

    @Test
    fun testPathInheritsFillFromGrandparentGroup() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <g fill="#ff0000">
            |    <g>
            |      <path d="M0,0l2,3Z" />
            |    </g>
            |  </g>
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val outerGroup = graphic.elements.first() as Group
        val innerGroup = outerGroup.elements.first() as Group
        val path = innerGroup.elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0xFFFF0000u))
    }

    @Test
    fun testPathAttributeOverridesGroupFill() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <g fill="#ff0000">
            |    <path fill="#00ff00" d="M0,0l2,3Z" />
            |  </g>
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val group = graphic.elements.first() as Group
        val path = group.elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0xFF00FF00u))
    }

    @Test
    fun testPathStyleOverridesGroupFill() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <g fill="#ff0000">
            |    <path style="fill:#0000ff" d="M0,0l2,3Z" />
            |  </g>
            |</svg>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        val graphic = parse(document.firstChild)
        val group = graphic.elements.first() as Group
        val path = group.elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0xFF0000FFu))
    }

    @Test
    fun testParseUnknownElementWithChildren() {
        val vectorText =
            """
            |<svg>
            |  <bicycle>
            |    <path d="M0,0l2,3Z" />
            |  </bicycle>
            |</svg>
            |
            """.trimMargin().toByteArray()

        val expectedChild =
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    LineTo(CommandVariant.RELATIVE, listOf(Point(2f, 3f))),
                    ClosePath,
                ),
            )

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use { input ->
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                    normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        assertThat(graphic::elements).index(0).isInstanceOf(Extra::class).all {
            prop(Extra::name).isEqualTo("bicycle")
            prop(Extra::elements).containsExactly(expectedChild)
        }
    }
}
