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
import assertk.assertions.isNotNull
import assertk.assertions.prop
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.TileMode
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Rect
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
    fun testExtractedStylePropertiesRemovedFromForeign() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <path style="fill:red" d="M0,0l2,3Z" />
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

    @Test
    fun testUnextractedStylePropertiesPreservedInForeignOnShape() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <rect x="0" y="0" width="10" height="10" style="opacity:0.5;fill:red;display:inline" />
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
        val rect = graphic.elements.first() as Rect

        assertThat(rect::fillBrush).isEqualTo(Color(0xFFFF0000u))
        val style = rect.foreign["style"]
        assertThat(style).isNotNull()
        val styleProps = style!!.split(';').map { it.trim() }.toSet()
        assertThat(styleProps).contains("opacity:0.5")
        assertThat(styleProps).contains("display:inline")
        assertThat(styleProps).containsNone("fill:red")
    }

    @Test
    fun testGroupClipPathRefIsResolved() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <defs>
            |    <clipPath id="cp1">
            |      <path d="M0,0h10v10h-10z" />
            |    </clipPath>
            |  </defs>
            |  <g clip-path="url(#cp1)">
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
        val group = graphic.elements.filterIsInstance<Group>().first()

        assertThat(group::clipPaths).hasSize(1)
        assertThat(group.clipPaths.first()::id).isEqualTo("cp1")
    }

    @Test
    fun testGroupClipPathRefUnresolvedWhenIdMissing() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <g clip-path="url(#missing)">
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
        val group = graphic.elements.filterIsInstance<Group>().first()

        assertThat(group::clipPaths).isEmpty()
        assertThat(group.foreign["clip-path"]).isEqualTo("url(#missing)")
    }

    @Test
    fun testUnextractedStylePropertiesPreservedInForeignOnPath() {
        val vectorText =
            """
            |<svg viewBox="0 0 100 100">
            |  <path d="M0,0l2,3Z" style="opacity:0.75;stroke:blue" />
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

        assertThat(path::stroke).isEqualTo(Color(0xFF0000FFu))
        assertThat(path.foreign["style"]).isEqualTo("opacity:0.75")
    }

    @Test
    fun testUserSpaceLinearGradientFillParsed() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <defs>
                |    <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="24" y2="0">
                |      <stop offset="0" stop-color="#b125ea"/>
                |      <stop offset="0.5" stop-color="#833fef"/>
                |      <stop offset="1" stop-color="#008aff"/>
                |    </linearGradient>
                |  </defs>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.first() as Path
        val expected =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(0.5f, Color(0xFF833FEFu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        assertThat(path::fill).isEqualTo(expected)
    }

    @Test
    fun testConsumedGradientDefAndEmptyDefsArePruned() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <defs>
                |    <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="24" y2="0">
                |      <stop offset="0" stop-color="#b125ea"/>
                |      <stop offset="1" stop-color="#008aff"/>
                |    </linearGradient>
                |  </defs>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        assertThat(graphic.elements.filterIsInstance<Extra>(), "extra elements").isEmpty()
    }

    @Test
    fun testGradientTransformIsBakedIntoCoordinates() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <linearGradient id="g" x1="0" y1="0" x2="1" y2="0" gradientUnits="userSpaceOnUse"
                |      gradientTransform="matrix(2,0,0,4,10,20)">
                |    <stop offset="0" stop-color="#b125ea"/>
                |    <stop offset="1" stop-color="#008aff"/>
                |  </linearGradient>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.first() as Path
        val fill = path.fill as LinearGradient
        assertThat(fill::startX).isEqualTo(10f)
        assertThat(fill::startY).isEqualTo(20f)
        assertThat(fill::endX).isEqualTo(12f)
        assertThat(fill::endY).isEqualTo(20f)
    }

    @Test
    fun testObjectBoundingBoxGradientResolvesAgainstElementBounds() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 200 200">
                |  <linearGradient id="g">
                |    <stop offset="0" stop-color="#b125ea"/>
                |    <stop offset="1" stop-color="#008aff"/>
                |  </linearGradient>
                |  <rect x="10" y="20" width="100" height="50" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val rect = graphic.elements.first() as Rect
        val fill = rect.fillBrush as LinearGradient
        assertThat(fill::startX).isEqualTo(10f)
        assertThat(fill::startY).isEqualTo(20f)
        assertThat(fill::endX).isEqualTo(110f)
        assertThat(fill::endY).isEqualTo(20f)
    }

    @Test
    fun testObjectBoundingBoxGradientResolvesAgainstPathBounds() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 200 200">
                |  <linearGradient id="g">
                |    <stop offset="0" stop-color="#b125ea"/>
                |    <stop offset="1" stop-color="#008aff"/>
                |  </linearGradient>
                |  <path d="M20,30L70,30L70,90Z" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.first() as Path
        val fill = path.fill as LinearGradient
        assertThat(fill::startX).isEqualTo(20f)
        assertThat(fill::startY).isEqualTo(30f)
        assertThat(fill::endX).isEqualTo(70f)
        assertThat(fill::endY).isEqualTo(30f)
    }

    @Test
    fun testObjectBoundingBoxGradientOnEmptyPathFallsBackToPassthrough() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 200 200">
                |  <linearGradient id="g">
                |    <stop offset="0" stop-color="#b125ea"/>
                |    <stop offset="1" stop-color="#008aff"/>
                |  </linearGradient>
                |  <path d="" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.filterIsInstance<Path>().first()
        assertThat(path::fill).isEqualTo(Colors.BLACK)
        assertThat(path.foreign["fill"]).isEqualTo("url(#g)")
        assertThat(graphic.elements.filterIsInstance<Extra>().map { it.id }, "extra element ids").containsExactly("g")
    }

    @Test
    fun testRadialGradientStrokeParsed() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <radialGradient id="g" gradientUnits="userSpaceOnUse" cx="12" cy="12" r="6" spreadMethod="reflect">
                |    <stop offset="0" stop-color="#b125ea"/>
                |    <stop offset="1" stop-color="#008aff"/>
                |  </radialGradient>
                |  <path d="M0,0L24,0L24,24Z" fill="none" stroke="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.filterIsInstance<Path>().first()
        val expected =
            RadialGradient(
                centerX = 12f,
                centerY = 12f,
                radius = 6f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
                tileMode = TileMode.MIRROR,
            )
        assertThat(path::stroke).isEqualTo(expected)
    }

    @Test
    fun testGradientStopsParsedFromStyleWithPercentOffsetsAndOpacity() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="24" y2="0" spreadMethod="repeat">
                |    <stop offset="50%" style="stop-color:rgb(255,0,0);stop-opacity:0.5"/>
                |    <stop offset="100%" style="stop-color:#008aff"/>
                |  </linearGradient>
                |  <path d="M0,0L24,0L24,24Z" style="fill:url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.first() as Path
        val expected =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0.5f, Color(0x80FF0000u)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
                tileMode = TileMode.REPEAT,
            )
        assertThat(path::fill).isEqualTo(expected)
    }

    @Test
    fun testSingleStopGradientCollapsesToSolidColor() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <linearGradient id="g">
                |    <stop offset="0" stop-color="#b125ea"/>
                |  </linearGradient>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.first() as Path
        assertThat(path::fill).isEqualTo(Color(0xFFB125EAu))
    }

    @Test
    fun testStoplessGradientRendersAsNoFill() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <linearGradient id="g"/>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.first() as Path
        assertThat(path::fill).isEqualTo(Colors.TRANSPARENT)
    }

    @Test
    fun testHrefTemplateSuppliesStops() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <linearGradient id="base">
                |    <stop offset="0" stop-color="#b125ea"/>
                |    <stop offset="1" stop-color="#008aff"/>
                |  </linearGradient>
                |  <linearGradient id="derived" href="#base" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="10" y2="0"/>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#derived)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.filterIsInstance<Path>().first()
        val expected =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 10f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        assertThat(path::fill).isEqualTo(expected)

        // The consumed def is pruned; the unreferenced template is conservatively kept
        val extras = graphic.elements.filterIsInstance<Extra>()
        assertThat(extras.map { it.id }, "extra element ids").containsExactly("base")
    }

    @Test
    fun testCyclicHrefFallsBackToPassthrough() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <linearGradient id="a" href="#b"/>
                |  <linearGradient id="b" href="#a"/>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#a)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.filterIsInstance<Path>().first()
        assertThat(path::fill).isEqualTo(Colors.BLACK)
        assertThat(path.foreign["fill"]).isEqualTo("url(#a)")
        assertThat(graphic.elements.filterIsInstance<Extra>(), "extra elements").hasSize(2)
    }

    @Test
    fun testUnrepresentableGradientTransformFallsBackToPassthrough() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <defs>
                |    <linearGradient id="skewed" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="1" y2="0"
                |        gradientTransform="matrix(1,1,0,1,0,0)">
                |      <stop offset="0" stop-color="#b125ea"/>
                |      <stop offset="1" stop-color="#008aff"/>
                |    </linearGradient>
                |  </defs>
                |  <path d="M0,0L24,0L24,24Z" style="fill:url(#skewed)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.filterIsInstance<Path>().first()
        assertThat(path::fill).isEqualTo(Colors.BLACK)
        assertThat(path.foreign["style"]).isEqualTo("fill:url(#skewed)")

        val defs = graphic.elements.filterIsInstance<Extra>().single()
        assertThat(defs::name).isEqualTo("defs")
        assertThat(defs.elements.filterIsInstance<Extra>().map { it.id }, "def ids").containsExactly("skewed")
    }

    @Test
    fun testPartiallyResolvableGradientDefIsKept() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="24" y2="0">
                |    <stop offset="0" stop-color="#b125ea"/>
                |    <stop offset="1" stop-color="#008aff"/>
                |  </linearGradient>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#g)"/>
                |  <g fill="url(#g)">
                |    <circle cx="12" cy="12" r="6"/>
                |  </g>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.filterIsInstance<Path>().first()
        assertThat(path.fill, "path fill").isInstanceOf(LinearGradient::class)

        // The group's reference is not resolved, so the def must survive
        val group = graphic.elements.filterIsInstance<Group>().single()
        assertThat(group.foreign["fill"]).isEqualTo("url(#g)")
        assertThat(graphic.elements.filterIsInstance<Extra>().map { it.id }, "extra element ids").containsExactly("g")
    }

    @Test
    fun testMissingGradientReferenceFallsBackToPassthrough() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <path d="M0,0L24,0L24,24Z" fill="url(#missing)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.first() as Path
        assertThat(path::fill).isEqualTo(Colors.BLACK)
        assertThat(path.foreign["fill"]).isEqualTo("url(#missing)")
    }

    @Test
    fun testUserSpacePercentageCoordinatesFallBackToPassthrough() {
        val graphic =
            parseSvg(
                """
                |<svg viewBox="0 0 24 24">
                |  <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="50%" y2="0">
                |    <stop offset="0" stop-color="#b125ea"/>
                |    <stop offset="1" stop-color="#008aff"/>
                |  </linearGradient>
                |  <path d="M0,0L24,0L24,24Z" fill="url(#g)"/>
                |</svg>
                |
                """.trimMargin(),
            )

        val path = graphic.elements.filterIsInstance<Path>().first()
        assertThat(path::fill).isEqualTo(Colors.BLACK)
        assertThat(path.foreign["fill"]).isEqualTo("url(#g)")
        assertThat(graphic.elements.filterIsInstance<Extra>(), "extra elements").hasSize(1)
    }

    private fun parseSvg(text: String): Graphic {
        val document =
            ByteArrayInputStream(text.toByteArray()).use { input ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(input)
                    .apply { normalize() }
            }

        return parse(document.firstChild)
    }
}
