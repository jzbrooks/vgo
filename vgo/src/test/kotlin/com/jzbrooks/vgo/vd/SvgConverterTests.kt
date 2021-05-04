package com.jzbrooks.vgo.vd

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.vd.graphic.ClipPath
import org.junit.jupiter.api.Test

class SvgConverterTests {
    val topLevelAttributes = VectorDrawable.Attributes(
        "visibilitystrike",
        mutableMapOf(
            "xmlns:android" to "http://schemas.android.com/apk/res/android",
            "android:height" to "24dp",
            "android:width" to "24dp",
            "android:viewportHeight" to "24",
            "android:viewportWidth" to "24"
        )
    )

    @Test
    fun testRootElementAttributesAreConverted() {
        val vectorDrawable = VectorDrawable(
            listOf(
                Path(CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(), Path.Attributes("strike_thru_path", mutableMapOf()))
            ),
            topLevelAttributes
        )

        val svg = vectorDrawable.toSvg()

        assertThat(svg.attributes.name).isEqualTo("visibilitystrike")
        assertThat(svg.attributes.foreign).containsOnly(
            "xmlns" to "http://www.w3.org/2000/svg",
            "height" to "100%",
            "width" to "100%",
            "viewPort" to "0 0 24 24"
        )
    }

    @Test
    fun testSinglePathElementAttributesAreConverted() {
        val attributes = Path.Attributes(
            "strike_thru_path",
            mutableMapOf(
                "android:strokeWidth" to "10"
            )
        )

        val pathElementGraphic = VectorDrawable(
            listOf(
                Path(CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(), attributes)
            ),
            topLevelAttributes
        )

        val graphic = pathElementGraphic.toSvg()

        val first = graphic.elements.first()
        assertThat(first.attributes.name).isEqualTo("strike_thru_path")
        assertThat(first.attributes.foreign).containsOnly("stroke-width" to "10")
    }

    @Test
    fun testHexColorsWithAlphaAreConverted() {
        val attributes = Path.Attributes(
            "strike_thru_path",
            mutableMapOf(
                "android:fillColor" to "#FFFF00FF",
                "android:strokeWidth" to "10"
            )
        )

        val pathElementGraphic = VectorDrawable(
            listOf(
                Path(CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(), attributes)
            ),
            topLevelAttributes
        )

        val graphic = pathElementGraphic.toSvg()

        val first = graphic.elements.first()
        assertThat(first.attributes.name).isEqualTo("strike_thru_path")
        assertThat(first.attributes.foreign).containsOnly(
            "stroke-width" to "10",
            "fill" to "#FF00FF",
        )
    }

    @Test
    fun testClipPathDefsAreAdded() {
        val pathElementGraphic = VectorDrawable(
            listOf(
                Path(CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(), Path.Attributes("strike_thru_path", mutableMapOf())),
                ClipPath(CommandString("M 0 0 L 24 0 L 24 24 L 0 24 L 0 0 Z M 4.54 1.73 L 3.27 3 L 3.27 3 L 4.54 1.73 Z").toCommandList()),
                Path(CommandString("M 12 4.5 C 7 4.5 2.73 7.61 1 12 C 2.73 16.39 7 19.5 12 19.5 C 17 19.5 21.27 16.39 23 12 C 21.27 7.61 17 4.5 12 4.5 L 12 4.5 Z M 12 17 C 9.24 17 7 14.76 7 12 C 7 9.24 9.24 7 12 7 C 14.76 7 17 9.24 17 12 C 17 14.76 14.76 17 12 17 L 12 17 Z M 12 9 C 10.34 9 9 10.34 9 12 C 9 13.66 10.34 15 12 15 C 13.66 15 15 13.66 15 12 C 15 10.34 13.66 9 12 9 L 12 9 Z").toCommandList())
            ),
            topLevelAttributes
        )

        val graphic = pathElementGraphic.toSvg()

        val clipPaths = graphic.elements.filterIsInstance<Extra>()
        val paths = graphic.elements.filterIsInstance<Path>()

        assertThat(paths.filter { it.attributes.foreign.containsKey("clip-path") }.size).isLessThan(paths.size)
        assertThat(clipPaths).hasSize(1)
    }

    @Test
    fun testContainerElementTransformsConverted() {
        val groupTransformGraphic = VectorDrawable(
            listOf(
                Group(
                    listOf(
                        Path(
                            CommandString("M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33V9h4.93L13,7v2h4V5.33C17,4.6 16.4,4 15.67,4Z").toCommandList(),
                            Path.Attributes(
                                "vect",
                                mutableMapOf(

                                    "android:fillColor" to "#FF000000",
                                    "android:fillAlpha" to ".3",
                                )
                            )
                        ),
                        Path(
                            CommandString("M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5Z").toCommandList(),
                            Path.Attributes("vect", mutableMapOf("android:fillColor" to "#FF000000"))
                        )
                    ),
                    Group.Attributes(
                        "rotationGroup",
                        mutableMapOf(
                            "android:pivotX" to "10.0",
                            "android:pivotY" to "10.0",
                            "android:rotation" to "15.0"
                        )
                    )
                )
            ),
            topLevelAttributes
        )

        val graphic = groupTransformGraphic.toSvg()

        val groupAttributes = (graphic.elements.first() as Group).attributes.foreign

        assertThat(groupAttributes).contains("transform", "matrix(0.9659258, 0.25881904, -0.25881904, 0.9659258, 2.9289327, -2.247448)")
    }

    @Test
    fun testContainerElementAttributesConverted() {
        val groupTransformGraphic = VectorDrawable(
            listOf(
                Group(
                    listOf(
                        Path(
                            CommandString("M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33V9h4.93L13,7v2h4V5.33C17,4.6 16.4,4 15.67,4Z").toCommandList(),
                            Path.Attributes(
                                "vect",
                                mutableMapOf(

                                    "android:fillColor" to "#FF000000",
                                    "android:fillAlpha" to ".3",
                                )
                            )
                        ),
                        Path(
                            CommandString("M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5Z").toCommandList(),
                            Path.Attributes("vect", mutableMapOf("android:fillColor" to "#FF000000"))
                        )
                    ),
                    Group.Attributes(
                        "rotationGroup",
                        mutableMapOf(
                            "android:pivotX" to "10.0",
                            "android:pivotY" to "10.0",
                            "android:rotation" to "15.0"
                        )
                    )
                )
            ),
        )

        val graphic = groupTransformGraphic.toSvg()

        val groupAttributes = (graphic.elements.first() as Group).attributes

        assertThat(groupAttributes.name).isEqualTo("rotationGroup")
    }
}
