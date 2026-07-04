package com.jzbrooks.vgo.svg

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSameSizeAs
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.SweepGradient
import com.jzbrooks.vgo.core.TileMode
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Polygon
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.util.assertk.attribute
import com.jzbrooks.vgo.util.assertk.doesNotHaveAttribute
import com.jzbrooks.vgo.util.assertk.hasName
import com.jzbrooks.vgo.util.assertk.hasNames
import com.jzbrooks.vgo.util.assertk.hasValue
import com.jzbrooks.vgo.util.element.createPath
import com.jzbrooks.vgo.util.xml.toList
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory

class ScalableVectorGraphicWriterTests {
    @Test
    fun testRootElementIsSvg() {
        ByteArrayOutputStream().use {
            ScalableVectorGraphicWriter().write(graphic, it)

            val output = it.toDocument()
            assertThat(output.firstChild, "first document element").hasName("svg")
        }
    }

    @Test
    fun testIsViewboxWritten() {
        ByteArrayOutputStream().use {
            ScalableVectorGraphicWriter().write(graphic, it)

            val output = it.toDocument()
            val root = output.firstChild

            assertThat(root).attribute("viewbox").isEqualTo("0 0 100 100")
        }
    }

    @Test
    fun testPathsWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.firstChild.childNodes.toList()

            assertThat(firstGenNodes)
                .transform("path element count") { it.count { item -> item.nodeName == "path" } }
                .isEqualTo(2)
        }
    }

    @Test
    fun testElementOrderMaintained() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.firstChild.childNodes.toList()

            assertThat(firstGenNodes).hasNames("path", "bicycle", "path")
        }
    }

    @Test
    fun testVectorAttributesWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rootNode = output.firstChild

            assertThat(rootNode.attributes.length).isEqualTo(graphic.foreign.size)
        }
    }

    @Test
    fun testPathMetadataWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstPathNode = output.firstChild.firstChild

            assertThat(firstPathNode).attribute("id").isEqualTo("strike_thru_path")
        }
    }

    @Test
    fun testGroupChildrenWritten() {
        val graphicWithGroup =
            ScalableVectorGraphic(
                listOf(Group(graphic.elements)),
                null,
                mutableMapOf(
                    "xmlns" to "http://www.w3.org/2000/svg",
                    "viewbox" to "0 0 100 100",
                ),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val groupChildren =
                output.firstChild.firstChild.childNodes
                    .toList()

            assertThat(groupChildren).hasSameSizeAs(graphic.elements)
        }
    }

    @Test
    fun testExtraWritten() {
        val graphicWithGroup =
            ScalableVectorGraphic(
                graphic.elements,
                null,
                mutableMapOf(
                    "xmlns" to "http://www.w3.org/2000/svg",
                    "viewbox" to "0 0 100 100",
                ),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val extraNode = output.firstChild.childNodes.item(1)

            assertThat(extraNode).hasName("bicycle")
        }
    }

    @Test
    fun testExtraChildrenWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val extraChildren =
                output.firstChild.childNodes
                    .item(1)
                    .childNodes
                    .toList()

            assertThat(extraChildren).index(0).hasName("g")
        }
    }

    @Test
    fun testRedundantFillOmittedWhenMatchesParentGroup() {
        val graphicWithGroup =
            ScalableVectorGraphic(
                listOf(
                    Group(
                        listOf(createPath(fill = Color(0xFFFF0000u))),
                        foreign = mutableMapOf("fill" to "red"),
                    ),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val pathNode = output.firstChild.firstChild.firstChild

            assertThat(pathNode).doesNotHaveAttribute("fill")
        }
    }

    @Test
    fun testFillWrittenWhenDiffersFromParentGroup() {
        val graphicWithGroup =
            ScalableVectorGraphic(
                listOf(
                    Group(
                        listOf(createPath(fill = Colors.BLACK)),
                        foreign = mutableMapOf("fill" to "red"),
                    ),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val pathNode = output.firstChild.firstChild.firstChild

            assertThat(pathNode).attribute("fill").isEqualTo("black")
        }
    }

    @Test
    fun testDefaultFillOmittedOnGroup() {
        val graphicWithGroup =
            ScalableVectorGraphic(
                listOf(
                    Group(
                        listOf(createPath()),
                        foreign = mutableMapOf("fill" to "black"),
                    ),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val groupNode = output.firstChild.firstChild

            assertThat(groupNode).doesNotHaveAttribute("fill")
        }
    }

    @Test
    fun testStrokeNoneWrittenWhenParentGroupHasStroke() {
        val graphicWithGroup =
            ScalableVectorGraphic(
                listOf(
                    Group(
                        listOf(createPath(stroke = Colors.TRANSPARENT)),
                        foreign = mutableMapOf("stroke" to "red"),
                    ),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val pathNode = output.firstChild.firstChild.firstChild

            assertThat(pathNode).attribute("stroke").isEqualTo("none")
        }
    }

    @Test
    fun testCirclePaintAttributesWritten() {
        val circle =
            Circle(
                id = null,
                foreign = mutableMapOf(),
                cx = 5f,
                cy = 5f,
                r = 3f,
                fill = Color(0xFFFF0000u),
                fillRule = Path.FillRule.NON_ZERO,
                stroke = Color(0xFF0000FFu),
                strokeWidth = 2f,
                strokeLineCap = Path.LineCap.BUTT,
                strokeLineJoin = Path.LineJoin.MITER,
                strokeMiterLimit = 4f,
            )
        val graphic =
            ScalableVectorGraphic(
                listOf(circle),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val circleNode = output.firstChild.firstChild

            assertThat(circleNode).all {
                hasName("circle")
                attribute("fill").isEqualTo("red")
                attribute("stroke").isEqualTo("blue")
                attribute("stroke-width").isEqualTo("2")
            }
        }
    }

    @Test
    fun testRectTransparentFillWrittenAsNone() {
        val rect =
            Rect(
                id = null,
                foreign = mutableMapOf(),
                x = 0f,
                y = 0f,
                width = 10f,
                height = 10f,
                rx = 0f,
                ry = 0f,
                fill = Colors.TRANSPARENT,
                fillRule = Path.FillRule.NON_ZERO,
                stroke = Colors.BLACK,
                strokeWidth = 1f,
                strokeLineCap = Path.LineCap.BUTT,
                strokeLineJoin = Path.LineJoin.MITER,
                strokeMiterLimit = 4f,
            )
        val graphic =
            ScalableVectorGraphic(
                listOf(rect),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rectNode = output.firstChild.firstChild

            assertThat(rectNode).attribute("fill").isEqualTo("none")
        }
    }

    @Test
    fun testPolygonInheritsFillFromParentGroup() {
        val polygon =
            Polygon(
                id = null,
                foreign = mutableMapOf(),
                points = listOf(Point(0f, 0f), Point(5f, 0f), Point(5f, 5f)),
                fill = Color(0xFFFF0000u),
                fillRule = Path.FillRule.NON_ZERO,
                stroke = Colors.TRANSPARENT,
                strokeWidth = 1f,
                strokeLineCap = Path.LineCap.BUTT,
                strokeLineJoin = Path.LineJoin.MITER,
                strokeMiterLimit = 4f,
            )
        val graphic =
            ScalableVectorGraphic(
                listOf(
                    Group(
                        listOf(polygon),
                        foreign = mutableMapOf("fill" to "red"),
                    ),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val polygonNode = output.firstChild.firstChild.firstChild

            assertThat(polygonNode).all {
                hasName("polygon")
                doesNotHaveAttribute("fill")
            }
        }
    }

    @Test
    fun testLinearGradientFillWritesDefAndReference() {
        val gradient =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        val graphic = createGraphicWithBrush(fill = gradient)

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val root = memoryStream.toDocument().firstChild
            val defs = root.childNodes.toList().single { it.nodeName == "defs" }
            val gradientNode = defs.childNodes.toList().single { it.nodeName == "linearGradient" }
            assertThat(gradientNode).all {
                attribute("id").isEqualTo("gradient0")
                attribute("gradientUnits").isEqualTo("userSpaceOnUse")
                attribute("x1").isEqualTo("0")
                attribute("y1").isEqualTo("0")
                attribute("x2").isEqualTo("24")
                attribute("y2").isEqualTo("0")
                doesNotHaveAttribute("spreadMethod")
            }

            val stops = gradientNode.childNodes.toList().filter { it.nodeName == "stop" }
            assertThat(stops).hasSize(2)
            assertThat(stops[0]).all {
                attribute("offset").isEqualTo("0")
                attribute("stop-color").isEqualTo("#b125ea")
                doesNotHaveAttribute("stop-opacity")
            }

            val path = root.childNodes.toList().single { it.nodeName == "path" }
            assertThat(path).attribute("fill").isEqualTo("url(#gradient0)")
        }
    }

    @Test
    fun testRadialGradientStrokeWritesDefAndReference() {
        val gradient =
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
        val graphic = createGraphicWithBrush(stroke = gradient)

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val root = memoryStream.toDocument().firstChild
            val defs = root.childNodes.toList().single { it.nodeName == "defs" }
            val gradientNode = defs.childNodes.toList().single { it.nodeName == "radialGradient" }
            assertThat(gradientNode).all {
                attribute("cx").isEqualTo("12")
                attribute("cy").isEqualTo("12")
                attribute("r").isEqualTo("6")
                attribute("spreadMethod").isEqualTo("reflect")
            }

            val path = root.childNodes.toList().single { it.nodeName == "path" }
            assertThat(path).attribute("stroke").isEqualTo("url(#gradient0)")
        }
    }

    @Test
    fun testEqualGradientsShareOneDef() {
        val gradient =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        val graphic =
            ScalableVectorGraphic(
                listOf(
                    createPath(CommandString("M0,0L24,0Z").toCommandList(), fill = gradient),
                    createPath(CommandString("M0,24L24,24Z").toCommandList(), fill = gradient.copy()),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val root = memoryStream.toDocument().firstChild
            val defs = root.childNodes.toList().single { it.nodeName == "defs" }
            assertThat(defs.childNodes.toList().count { it.nodeName == "linearGradient" }, "gradient def count").isEqualTo(1)

            val paths = root.childNodes.toList().filter { it.nodeName == "path" }
            for (path in paths) {
                assertThat(path).attribute("fill").isEqualTo("url(#gradient0)")
            }
        }
    }

    @Test
    fun testTranslucentStopWritesStopOpacity() {
        val gradient =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0x80FF0000u)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        val graphic = createGraphicWithBrush(fill = gradient)

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val root = memoryStream.toDocument().firstChild
            val defs = root.childNodes.toList().single { it.nodeName == "defs" }
            val gradientNode = defs.childNodes.toList().single { it.nodeName == "linearGradient" }
            val stops = gradientNode.childNodes.toList().filter { it.nodeName == "stop" }

            assertThat(stops[0]).all {
                attribute("stop-color").isEqualTo("red")
                attribute("stop-opacity").isEqualTo(".502")
            }
            assertThat(stops[1]).doesNotHaveAttribute("stop-opacity")
        }
    }

    @Test
    fun testGradientAndClipPathShareSingleDefsElement() {
        val gradient =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        val graphic =
            ScalableVectorGraphic(
                listOf(
                    Group(
                        listOf(createPath(CommandString("M0,0L24,0Z").toCommandList(), fill = gradient)),
                        null,
                        mutableMapOf(),
                        clipPaths = listOf(ClipPath(listOf(createPath(CommandString("M0,0h24v24h-24z").toCommandList())))),
                    ),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val root = memoryStream.toDocument().firstChild
            val defsElements = root.childNodes.toList().filter { it.nodeName == "defs" }
            assertThat(defsElements, "defs elements").hasSize(1)

            val defChildren =
                defsElements
                    .single()
                    .childNodes
                    .toList()
                    .map { it.nodeName }
            assertThat(defChildren, "def children").isEqualTo(listOf("clipPath", "linearGradient"))
        }
    }

    @Test
    fun testGeneratedGradientIdsAvoidPassthroughDefIds() {
        val gradient =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        val graphic =
            ScalableVectorGraphic(
                listOf(
                    Extra("linearGradient", emptyList(), "gradient0", mutableMapOf()),
                    createPath(CommandString("M0,0L24,0Z").toCommandList(), fill = gradient),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val root = memoryStream.toDocument().firstChild
            val path = root.childNodes.toList().single { it.nodeName == "path" }
            assertThat(path).attribute("fill").isEqualTo("url(#gradient1)")
        }
    }

    @Test
    fun testShapeGradientBrushWritesDefAndReference() {
        val gradient =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        val rect =
            Rect(
                null,
                mutableMapOf(),
                0f,
                0f,
                24f,
                24f,
                0f,
                0f,
                Colors.BLACK,
                Path.FillRule.NON_ZERO,
                Colors.TRANSPARENT,
                1f,
                Path.LineCap.BUTT,
                Path.LineJoin.MITER,
                4f,
            ).apply { fillBrush = gradient }
        val graphic =
            ScalableVectorGraphic(
                listOf(rect),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val root = memoryStream.toDocument().firstChild
            val defs = root.childNodes.toList().single { it.nodeName == "defs" }
            assertThat(defs.childNodes.toList().count { it.nodeName == "linearGradient" }, "gradient def count").isEqualTo(1)

            val rectNode = root.childNodes.toList().single { it.nodeName == "rect" }
            assertThat(rectNode).attribute("fill").isEqualTo("url(#gradient0)")
        }
    }

    @Test
    fun testSweepGradientCannotBeWritten() {
        val gradient =
            SweepGradient(
                centerX = 12f,
                centerY = 12f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )
        val graphic = createGraphicWithBrush(fill = gradient)

        ByteArrayOutputStream().use { memoryStream ->
            assertFailure {
                ScalableVectorGraphicWriter().write(graphic, memoryStream)
            }.isInstanceOf(IllegalStateException::class)
        }
    }

    @Test
    fun testUnresolvedPaintReferenceInForeignIsWritten() {
        val graphic =
            ScalableVectorGraphic(
                listOf(
                    createPath(
                        CommandString("M0,0L24,0Z").toCommandList(),
                        foreign = mutableMapOf("fill" to "url(#external)"),
                    ),
                ),
                null,
                mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val root = memoryStream.toDocument().firstChild
            val path = root.childNodes.toList().single { it.nodeName == "path" }
            assertThat(path).attribute("fill").isEqualTo("url(#external)")
        }
    }

    private fun createGraphicWithBrush(
        fill: Brush = Colors.BLACK,
        stroke: Brush = Colors.TRANSPARENT,
    ) = ScalableVectorGraphic(
        listOf(
            createPath(
                CommandString("M0,0L24,0L24,24Z").toCommandList(),
                fill = fill,
                stroke = stroke,
            ),
        ),
        null,
        mutableMapOf("xmlns" to "http://www.w3.org/2000/svg"),
    )

    private fun ByteArrayOutputStream.toDocument(): Document =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(toByteArray()))

    companion object {
        val graphic =
            ScalableVectorGraphic(
                listOf(
                    createPath(
                        CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(),
                        "strike_thru_path",
                    ),
                    Extra(
                        "bicycle",
                        listOf(Group(emptyList())),
                        null,
                        mutableMapOf(),
                    ),
                    createPath(
                        CommandString(
                            "M 12 4.5 C 7 4.5 2.73 7.61 1 12 C 2.73 16.39 7 19.5 12 19.5 " +
                                "C 17 19.5 21.27 16.39 23 12 C 21.27 7.61 17 4.5 12 4.5 L 12 4.5 Z " +
                                "M 12 17 C 9.24 17 7 14.76 7 12 C 7 9.24 9.24 7 12 7 C 14.76 7 17 9.24 17 12 " +
                                "C 17 14.76 14.76 17 12 17 L 12 17 Z M 12 9 C 10.34 9 9 10.34 9 12 " +
                                "C 9 13.66 10.34 15 12 15 C 13.66 15 15 13.66 15 12 C 15 10.34 13.66 9 12 9 " +
                                "L 12 9 Z",
                        ).toCommandList(),
                    ),
                ),
                null,
                mutableMapOf(
                    "xmlns" to "http://www.w3.org/2000/svg",
                    "viewbox" to "0 0 100 100",
                ),
            )
    }
}
