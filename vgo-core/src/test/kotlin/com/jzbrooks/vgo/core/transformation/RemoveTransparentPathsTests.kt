package com.jzbrooks.vgo.core.transformation

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.ForeignPaint
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PaintInheritance
import com.jzbrooks.vgo.core.util.element.createGraphic
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.element.traverseTopDown
import org.junit.jupiter.api.Test

class RemoveTransparentPathsTests {
    private fun transform(
        graphic: Graphic,
        transformer: RemoveTransparentPaths = RemoveTransparentPaths(),
    ) {
        traverseTopDown(graphic, listOf(transformer))
    }

    @Test
    fun testTransparentPathsAreRemoved() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Colors.TRANSPARENT,
                        stroke = Colors.TRANSPARENT,
                    ),
                    createPath(),
                    createPath(),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements, "graphic elements").hasSize(2)
    }

    @Test
    fun testTransparentPathsWithIdsAreNotRemoved() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        id = "animatable",
                        fill = Colors.TRANSPARENT,
                        stroke = Colors.TRANSPARENT,
                    ),
                    createPath(),
                    createPath(),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements, "graphic elements").hasSize(3)
    }

    @Test
    fun testTransparentPathsWithForeignColorsAreNotRemoved() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Colors.TRANSPARENT,
                        stroke = Colors.TRANSPARENT,
                        foreign = mutableMapOf("android:strokeColor" to "?attrs/dark"),
                    ),
                    createPath(),
                    createPath(),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements, "graphic elements").hasSize(3)
    }

    @Test
    fun testTransparentPathsWithForeignPaintReferencesAreNotRemoved() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Colors.TRANSPARENT,
                        stroke = Colors.TRANSPARENT,
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                    createPath(),
                    createPath(),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements, "graphic elements").hasSize(3)
    }

    @Test
    fun testPathsPaintedByAnInheritedReferenceAreNotRemoved() {
        // The reference stays on the ancestor and the reader hands the child the CSS
        // initial value, so the child's typed paint reads as dead while the gradient
        // paints it.
        val graphic =
            createGraphic(
                listOf(
                    Group(
                        listOf(
                            createPath(
                                fill = Colors.TRANSPARENT,
                                stroke = Colors.TRANSPARENT,
                                strokeWidth = 2f,
                            ),
                        ),
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                ),
            )

        transform(graphic, RemoveTransparentPaths(paintServerInheritance))

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .hasSize(1)
    }

    @Test
    fun testPathsAreRemovedForFormatsThatDontInheritPaint() {
        val graphic =
            createGraphic(
                listOf(
                    Group(
                        listOf(
                            createPath(
                                fill = Colors.TRANSPARENT,
                                stroke = Colors.TRANSPARENT,
                                strokeWidth = 2f,
                            ),
                        ),
                        foreign = mutableMapOf("stroke" to "url(#gradient)"),
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .isEmpty()
    }

    @Test
    fun testTransparentPathsWithinPassthroughElementsAreNotRemoved() {
        val graphic =
            createGraphic(
                listOf(
                    Extra(
                        "mask",
                        listOf(
                            createPath(fill = Colors.TRANSPARENT, stroke = Colors.TRANSPARENT),
                            Group(listOf(createPath(fill = Colors.TRANSPARENT, stroke = Colors.TRANSPARENT))),
                        ),
                        null,
                        mutableMapOf(),
                    ),
                ),
            )

        transform(graphic)

        assertThat(graphic::elements).index(0).isInstanceOf<Extra>().all {
            // The passthrough element's own children are left exactly as they were read
            prop(Extra::elements).hasSize(2)
            prop(Extra::elements)
                .index(1)
                .isInstanceOf<Group>()
                .prop(Group::elements)
                .isEmpty()
        }
    }

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
