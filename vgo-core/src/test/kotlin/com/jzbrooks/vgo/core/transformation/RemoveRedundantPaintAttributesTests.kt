package com.jzbrooks.vgo.core.transformation

import assertk.all
import assertk.assertThat
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.TileMode
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.ForeignPaint
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PaintInheritance
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.util.element.createGraphic
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.element.traverseTopDown
import org.junit.jupiter.api.Test

class RemoveRedundantPaintAttributesTests {
    private fun transform(
        graphic: Graphic,
        transformer: RemoveRedundantPaintAttributes = RemoveRedundantPaintAttributes(),
    ) {
        traverseTopDown(graphic, listOf(transformer))
    }

    @Test
    fun testFillRuleIsResetWhenFillIsTransparent() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Colors.TRANSPARENT,
                        fillRule = Path.FillRule.EVEN_ODD,
                        stroke = Color(0xFF333333u),
                        strokeWidth = 0.7f,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Path>().all {
            prop(Path::fillRule).isEqualTo(Path.FillRule.NON_ZERO)
            prop(Path::strokeWidth).isEqualTo(0.7f)
        }
    }

    @Test
    fun testFillRuleIsKeptWhenFillIsVisible() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Color(0xFF333333u),
                        fillRule = Path.FillRule.EVEN_ODD,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::fillRule)
            .isEqualTo(Path.FillRule.EVEN_ODD)
    }

    @Test
    fun testFillRuleIsKeptWhenFillIsAGradient() {
        val gradient = LinearGradient(0f, 0f, 1f, 1f, emptyList(), TileMode.CLAMP)
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = gradient,
                        fillRule = Path.FillRule.EVEN_ODD,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::fillRule)
            .isEqualTo(Path.FillRule.EVEN_ODD)
    }

    @Test
    fun testStrokePropertiesAreResetWhenStrokeIsTransparent() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Color(0xFF333333u),
                        stroke = Colors.TRANSPARENT,
                        strokeWidth = 1f,
                        strokeLineCap = Path.LineCap.ROUND,
                        strokeLineJoin = Path.LineJoin.BEVEL,
                        strokeMiterLimit = 7f,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Path>().all {
            prop(Path::strokeWidth).isEqualTo(0f)
            prop(Path::strokeLineCap).isEqualTo(Path.LineCap.BUTT)
            prop(Path::strokeLineJoin).isEqualTo(Path.LineJoin.MITER)
            prop(Path::strokeMiterLimit).isEqualTo(4f)
        }
    }

    @Test
    fun testStrokeColorIsClearedWhenStrokeWidthIsZero() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Color(0xFF333333u),
                        stroke = Color(0xFF00FF00u),
                        strokeWidth = 0f,
                        strokeLineCap = Path.LineCap.SQUARE,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Path>().all {
            prop(Path::stroke).isEqualTo(Colors.TRANSPARENT)
            prop(Path::strokeLineCap).isEqualTo(Path.LineCap.BUTT)
        }
    }

    @Test
    fun testStrokePropertiesAreKeptWhenStrokeIsVisible() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        stroke = Color(0xFF333333u),
                        strokeWidth = 0.7f,
                        strokeLineCap = Path.LineCap.ROUND,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Path>().all {
            prop(Path::strokeWidth).isEqualTo(0.7f)
            prop(Path::strokeLineCap).isEqualTo(Path.LineCap.ROUND)
        }
    }

    @Test
    fun testMiterLimitIsResetForJoinsThatIgnoreIt() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        stroke = Color(0xFF333333u),
                        strokeWidth = 1f,
                        strokeLineJoin = Path.LineJoin.ROUND,
                        strokeMiterLimit = 7f,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::strokeMiterLimit)
            .isEqualTo(4f)
    }

    @Test
    fun testMiterLimitIsKeptForMiterClipJoins() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        stroke = Color(0xFF333333u),
                        strokeWidth = 1f,
                        strokeLineJoin = Path.LineJoin.MITER_CLIP,
                        strokeMiterLimit = 7f,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Path>().all {
            prop(Path::strokeLineJoin).isEqualTo(Path.LineJoin.MITER_CLIP)
            prop(Path::strokeMiterLimit).isEqualTo(7f)
        }
    }

    @Test
    fun testShapeBrushesSurviveCanonicalization() {
        // Shape.copy() drops fillBrush/strokeBrush, and Shape narrows the deprecated
        // stroke to Color, so the rewrite has to restore both by hand. Shapes reach the
        // transform only when ConvertShapesToPaths isn't in the pipeline.
        val gradient = LinearGradient(0f, 0f, 1f, 1f, emptyList(), TileMode.CLAMP)
        val circle =
            Circle(
                id = null,
                foreign = mutableMapOf(),
                cx = 5f,
                cy = 5f,
                r = 5f,
                fill = Colors.BLACK,
                fillRule = Path.FillRule.EVEN_ODD,
                stroke = Color(0xFF333333u),
                strokeWidth = 0f,
                strokeLineCap = Path.LineCap.ROUND,
                strokeLineJoin = Path.LineJoin.MITER,
                strokeMiterLimit = 7f,
            ).apply { fillBrush = gradient }

        val graphic = createGraphic(listOf(circle))

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Circle>().all {
            // A gradient fill is never treated as invisible, so the fill rule stays.
            prop(Circle::fillRule).isEqualTo(Path.FillRule.EVEN_ODD)
            prop(Circle::fillBrush).isEqualTo(gradient)
            prop(Circle::strokeBrush).isEqualTo(Colors.TRANSPARENT)
            prop(Circle::strokeLineCap).isEqualTo(Path.LineCap.BUTT)
            prop(Circle::strokeMiterLimit).isEqualTo(4f)
        }
    }

    @Test
    fun testNamedPathsAreNotModified() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        id = "animatable",
                        fill = Colors.TRANSPARENT,
                        fillRule = Path.FillRule.EVEN_ODD,
                        stroke = Colors.TRANSPARENT,
                        strokeWidth = 1f,
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Path>().all {
            prop(Path::fillRule).isEqualTo(Path.FillRule.EVEN_ODD)
            prop(Path::strokeWidth).isEqualTo(1f)
        }
    }

    @Test
    fun testUnparsedForeignFillIsNotModified() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Colors.TRANSPARENT,
                        fillRule = Path.FillRule.EVEN_ODD,
                        foreign = mutableMapOf("android:fillColor" to "?attr/colorPrimary"),
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::fillRule)
            .isEqualTo(Path.FillRule.EVEN_ODD)
    }

    @Test
    fun testUnresolvedPaintReferenceIsNotModified() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        stroke = Colors.TRANSPARENT,
                        strokeWidth = 2f,
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::strokeWidth)
            .isEqualTo(2f)
    }

    @Test
    fun testStrokeInStyleAttributeIsNotModified() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        stroke = Colors.TRANSPARENT,
                        strokeWidth = 2f,
                        foreign = mutableMapOf("style" to "stroke:#333;opacity:0.5"),
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::strokeWidth)
            .isEqualTo(2f)
    }

    @Test
    fun testStrokeQualifiersSurviveInheritedPaint() {
        val graphic =
            createGraphic(
                listOf(
                    Group(
                        listOf(deadLookingStroke()),
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                ),
            )

        transform(graphic, RemoveRedundantPaintAttributes(paintServerInheritance))

        assertThat(graphic::elements).index(0).isInstanceOf<Group>().prop(Group::elements).index(0).isInstanceOf<Path>().all {
            prop(Path::strokeWidth).isEqualTo(2f)
            prop(Path::strokeLineCap).isEqualTo(Path.LineCap.ROUND)
        }
    }

    @Test
    fun testInheritedPaintAccumulatesThroughNestedGroups() {
        val graphic =
            createGraphic(
                listOf(
                    Group(
                        listOf(Group(listOf(deadLookingStroke()))),
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                ),
            )

        transform(graphic, RemoveRedundantPaintAttributes(paintServerInheritance))

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .index(0)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::strokeWidth)
            .isEqualTo(2f)
    }

    @Test
    fun testInheritedPaintDoesNotLeakBetweenSiblingGroups() {
        val graphic =
            createGraphic(
                listOf(
                    Group(
                        listOf(deadLookingStroke()),
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                    Group(listOf(deadLookingStroke())),
                ),
            )

        transform(graphic, RemoveRedundantPaintAttributes(paintServerInheritance))

        assertThat(graphic::elements).all {
            index(0)
                .isInstanceOf<Group>()
                .prop(Group::elements)
                .index(0)
                .isInstanceOf<Path>()
                .prop(Path::strokeWidth)
                .isEqualTo(2f)
            index(1)
                .isInstanceOf<Group>()
                .prop(Group::elements)
                .index(0)
                .isInstanceOf<Path>()
                .prop(Path::strokeWidth)
                .isEqualTo(0f)
        }
    }

    @Test
    fun testTransformerCanBeReusedAcrossGraphics() {
        val transformer = RemoveRedundantPaintAttributes(paintServerInheritance)
        val inheritedGraphic =
            createGraphic(
                listOf(deadLookingStroke()),
                foreign = mutableMapOf("stroke" to "url(#gradient)"),
            )
        val independentGraphic = createGraphic(listOf(deadLookingStroke()))

        transform(inheritedGraphic, transformer)
        transform(independentGraphic, transformer)

        assertThat(inheritedGraphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::strokeWidth)
            .isEqualTo(2f)
        assertThat(independentGraphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::strokeWidth)
            .isEqualTo(0f)
    }

    @Test
    fun testModelablePaintClearsInheritedPaint() {
        val graphic =
            createGraphic(
                listOf(
                    Group(
                        listOf(
                            Group(
                                listOf(deadLookingStroke()),
                                foreign = mutableMapOf("stroke" to "none"),
                            ),
                        ),
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                ),
            )

        transform(graphic, RemoveRedundantPaintAttributes(paintServerInheritance))

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .index(0)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::strokeWidth)
            .isEqualTo(0f)
    }

    @Test
    fun testPaintIsNotInheritedForFormatsThatDontInheritIt() {
        val graphic =
            createGraphic(
                listOf(
                    Group(
                        listOf(deadLookingStroke()),
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::strokeWidth)
            .isEqualTo(0f)
    }

    @Test
    fun testGroupsWithinPassthroughElementsAreCanonicalized() {
        val graphic =
            createGraphic(
                listOf(
                    Extra(
                        "mask",
                        listOf(deadLookingStroke(), Group(listOf(deadLookingStroke()))),
                        null,
                        mutableMapOf(),
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Extra>().all {
            // The passthrough element's own children are left exactly as they were read
            prop(Extra::elements)
                .index(0)
                .isInstanceOf<Path>()
                .prop(Path::strokeWidth)
                .isEqualTo(2f)
            prop(Extra::elements)
                .index(1)
                .isInstanceOf<Group>()
                .prop(Group::elements)
                .index(0)
                .isInstanceOf<Path>()
                .prop(Path::strokeWidth)
                .isEqualTo(0f)
        }
    }

    // A stroke whose typed paint reads as dead, but whose qualifiers are worth keeping
    // when an ancestor turns out to paint it.
    private fun deadLookingStroke() =
        createPath(
            stroke = Colors.TRANSPARENT,
            strokeWidth = 2f,
            strokeLineCap = Path.LineCap.ROUND,
        )

    // Mirrors the SVG rule closely enough to exercise the transformation: a paint server
    // reference sets the channel and any other declaration clears it.
    private val paintServerInheritance =
        PaintInheritance { foreign, current ->
            ForeignPaint(
                fill = foreign["fill"]?.startsWith("url(") ?: current.fill,
                stroke = foreign["stroke"]?.startsWith("url(") ?: current.stroke,
            )
        }
}
